package com.informationplatform.hub.recommendation.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiPromptProfileMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiPromptVersionMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiPromptProfilePo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiPromptVersionPo;
import com.informationplatform.hub.identity.application.CurrentUserProvider;
import com.informationplatform.hub.identity.domain.AuthenticatedUser;
import com.informationplatform.hub.identity.infrastructure.persistence.mapper.UserAccountMapper;
import com.informationplatform.hub.identity.infrastructure.persistence.po.UserAccountPo;
import com.informationplatform.hub.recommendation.infrastructure.persistence.mapper.RecommendationRunMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.mapper.UserRecommendationProfileMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.po.RecommendationRunPo;
import com.informationplatform.hub.recommendation.infrastructure.persistence.po.UserRecommendationProfilePo;
import com.informationplatform.hub.recommendation.job.domain.JobRecommendationProfile;
import com.informationplatform.hub.recommendation.job.infrastructure.persistence.mapper.JobRecommendationProfileMapper;
import com.informationplatform.hub.recommendation.job.infrastructure.persistence.po.JobRecommendationProfilePo;
import com.informationplatform.hub.testing.DatabaseIntegrationTestSafety;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/** 在真实 MySQL 上验证 Profile 创建、完整替换、Owner 和无 Run 副作用。 */
@SpringBootTest
@EnabledIfEnvironmentVariable(
        named = "INFORMATION_HUB_TEST_DB_URL",
        matches = "jdbc:mysql://.+")
class RecommendationProfileServiceIntegrationTest {

    private static final String HASH_A = "a".repeat(64);

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        String databaseUrl = DatabaseIntegrationTestSafety.requireTestDatabase(
                required("INFORMATION_HUB_TEST_DB_URL"));
        registry.add("spring.datasource.url", () -> databaseUrl);
        registry.add("spring.datasource.username", () -> required("INFORMATION_HUB_TEST_DB_USERNAME"));
        registry.add("spring.datasource.password", () -> required("INFORMATION_HUB_TEST_DB_PASSWORD"));
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired private RecommendationProfileService profileService;
    @Autowired private UserAccountMapper userAccountMapper;
    @Autowired private AiPromptProfileMapper promptProfileMapper;
    @Autowired private AiPromptVersionMapper promptVersionMapper;
    @Autowired private UserRecommendationProfileMapper recommendationProfileMapper;
    @Autowired private JobRecommendationProfileMapper jobProfileMapper;
    @Autowired private RecommendationRunMapper recommendationRunMapper;

    @MockitoBean private CurrentUserProvider currentUserProvider;

    @Test
    @Transactional
    void createsUpdatesIsolatesOwnerKeepsStableHashAndDoesNotCreateRun() {
        UserAccountPo owner = insertUser("owner");
        UserAccountPo other = insertUser("other");
        AiPromptProfilePo ownedPrompt = insertUsablePrompt(owner.getId(), "Owned Prompt");
        AiPromptProfilePo otherPrompt = insertUsablePrompt(other.getId(), "Other Prompt");
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticated(owner));

        long runsBefore = recommendationRunMapper.selectCount(
                Wrappers.<RecommendationRunPo>lambdaQuery()
                        .eq(RecommendationRunPo::getUserId, owner.getId()));
        RecommendationProfileChange created = profileService.saveProfile(
                "JOB",
                command(ownedPrompt.getId(), List.of(" 大数据开发 ", "Java 后端", "Java 后端"), 15_000));

        assertTrue(created.created());
        assertEquals(List.of("Java 后端", "大数据开发"), created.profile().preferences().targetRoles());
        String stableHash = created.profile().core().contentHash();

        RecommendationProfileChange reordered = profileService.saveProfile(
                "JOB",
                command(ownedPrompt.getId(), List.of("Java 后端", "大数据开发"), 15_000));
        assertFalse(reordered.created());
        assertEquals(created.profile().core().id(), reordered.profile().core().id());
        assertEquals(stableHash, reordered.profile().core().contentHash());

        RecommendationProfileChange clearedSalary = profileService.saveProfile(
                "JOB",
                command(ownedPrompt.getId(), List.of("Java 后端", "大数据开发"), null));
        assertNull(clearedSalary.profile().preferences().salaryMinMonthlyYuan());
        assertFalse(stableHash.equals(clearedSalary.profile().core().contentHash()));
        assertEquals(
                runsBefore,
                recommendationRunMapper.selectCount(
                        Wrappers.<RecommendationRunPo>lambdaQuery()
                                .eq(RecommendationRunPo::getUserId, owner.getId())));

        RecommendationProfileNotFoundException crossOwner = assertThrows(
                RecommendationProfileNotFoundException.class,
                () -> profileService.saveProfile(
                        "JOB",
                        command(otherPrompt.getId(), List.of("Java 后端"), 20_000)));
        assertEquals("RECOMMENDATION_PROMPT_PROFILE_NOT_FOUND", crossOwner.getCode());
        assertEquals(
                ownedPrompt.getId(),
                recommendationProfileMapper
                        .selectOwnedByType(owner.getId(), "JOB")
                        .getAnalysisPromptProfileId());

        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticated(other));
        RecommendationProfileNotFoundException isolated = assertThrows(
                RecommendationProfileNotFoundException.class,
                () -> profileService.getProfile("JOB"));
        assertEquals("RECOMMENDATION_PROFILE_NOT_FOUND", isolated.getCode());
    }

    @Test
    @Transactional
    void validatesPromptUsabilityAndCoreLimitsBeforePersistence() {
        UserAccountPo owner = insertUser("validation");
        AiPromptProfilePo disabled = insertUsablePrompt(owner.getId(), "Disabled Prompt");
        disabled.setStatus("DISABLED");
        assertEquals(1, promptProfileMapper.updateById(disabled));
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticated(owner));

        RecommendationProfileRequestException disabledError = assertThrows(
                RecommendationProfileRequestException.class,
                () -> profileService.saveProfile(
                        "JOB", command(disabled.getId(), List.of("Java"), 15_000)));
        assertEquals("RECOMMENDATION_PROMPT_PROFILE_DISABLED", disabledError.getCode());

        RecommendationProfileRequestException limitError = assertThrows(
                RecommendationProfileRequestException.class,
                () -> profileService.saveProfile(
                        "JOB",
                        new SaveRecommendationProfileCommand(
                                disabled.getId(),
                                31,
                                50,
                                List.of(),
                                List.of(),
                                List.of(),
                                List.of(),
                                null,
                                List.of())));
        assertEquals("RECOMMENDATION_PROFILE_WINDOW_DAYS_INVALID", limitError.getCode());
        assertEquals(
                0L,
                recommendationProfileMapper.selectCount(
                        Wrappers.<UserRecommendationProfilePo>lambdaQuery()
                                .eq(UserRecommendationProfilePo::getUserId, owner.getId())));
    }

    private SaveRecommendationProfileCommand command(
            long promptProfileId, List<String> targetRoles, Integer salary) {
        return new SaveRecommendationProfileCommand(
                promptProfileId,
                7,
                50,
                targetRoles,
                List.of("Spring Boot", "Java"),
                List.of("成都"),
                List.of("remote", "HYBRID"),
                salary,
                List.of("纯销售"));
    }

    private UserAccountPo insertUser(String prefix) {
        UserAccountPo po = new UserAccountPo();
        po.setUsername("task035-" + prefix + "-" + UUID.randomUUID());
        po.setPasswordHash("$2a$12$task035-not-a-real-secret");
        po.setTimezone("Asia/Shanghai");
        assertEquals(1, userAccountMapper.insert(po));
        return po;
    }

    private AiPromptProfilePo insertUsablePrompt(long userId, String name) {
        AiPromptProfilePo profile = new AiPromptProfilePo();
        profile.setUserId(userId);
        profile.setName(name + " " + UUID.randomUUID());
        profile.setAnalysisDefinitionKey("JOB_USER_RELEVANCE");
        profile.setStatus("ACTIVE");
        assertEquals(1, promptProfileMapper.insert(profile));

        AiPromptVersionPo version = new AiPromptVersionPo();
        version.setPromptProfileId(profile.getId());
        version.setVersionNo(1);
        version.setContent("TASK-035 integration prompt");
        version.setContentHash(HASH_A);
        assertEquals(1, promptVersionMapper.insert(version));
        assertEquals(
                1,
                promptProfileMapper.updateActiveVersion(
                        profile.getId(), userId, version.getId()));
        profile.setActiveVersionId(version.getId());
        return profile;
    }

    private AuthenticatedUser authenticated(UserAccountPo user) {
        return new AuthenticatedUser(
                user.getId(), user.getUsername(), user.getDisplayName(), user.getTimezone());
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }
}
