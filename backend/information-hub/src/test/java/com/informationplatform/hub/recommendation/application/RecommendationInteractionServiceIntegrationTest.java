package com.informationplatform.hub.recommendation.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.informationplatform.hub.identity.application.CurrentUserProvider;
import com.informationplatform.hub.identity.domain.AuthenticatedUser;
import com.informationplatform.hub.identity.infrastructure.persistence.mapper.UserAccountMapper;
import com.informationplatform.hub.identity.infrastructure.persistence.po.UserAccountPo;
import com.informationplatform.hub.information.infrastructure.persistence.mapper.InformationItemMapper;
import com.informationplatform.hub.information.infrastructure.persistence.po.InformationItemPo;
import com.informationplatform.hub.recommendation.domain.FeedbackState;
import com.informationplatform.hub.recommendation.domain.UserInformationInteraction;
import com.informationplatform.hub.recommendation.infrastructure.persistence.mapper.UserInformationInteractionMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.po.UserInformationInteractionPo;
import com.informationplatform.hub.recommendation.job.domain.JobDisposition;
import com.informationplatform.hub.recommendation.job.infrastructure.persistence.mapper.UserJobDispositionMapper;
import com.informationplatform.hub.testing.DatabaseIntegrationTestSafety;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** 在真实 MySQL 上验证 current aggregate、独立状态、清除与并发 upsert。 */
@SpringBootTest
@EnabledIfEnvironmentVariable(
        named = "INFORMATION_HUB_TEST_DB_URL",
        matches = "jdbc:mysql://.+")
class RecommendationInteractionServiceIntegrationTest {

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        String databaseUrl = DatabaseIntegrationTestSafety.requireTestDatabase(
                required("INFORMATION_HUB_TEST_DB_URL"));
        registry.add("spring.datasource.url", () -> databaseUrl);
        registry.add("spring.datasource.username", () -> required("INFORMATION_HUB_TEST_DB_USERNAME"));
        registry.add("spring.datasource.password", () -> required("INFORMATION_HUB_TEST_DB_PASSWORD"));
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired private RecommendationInteractionService interactionService;
    @Autowired private UserAccountMapper userAccountMapper;
    @Autowired private InformationItemMapper informationItemMapper;
    @Autowired private UserInformationInteractionMapper interactionMapper;
    @Autowired private UserJobDispositionMapper jobDispositionMapper;

    @MockitoBean private CurrentUserProvider currentUserProvider;

    @Test
    void recordsReplacesClearsAndKeepsFeedbackIndependentFromDisposition() {
        TestOwnerInformation fixture = createFixture("states");
        try {
            UserInformationInteraction first =
                    interactionService.recordView(fixture.informationId(), null);
            UserInformationInteraction second =
                    interactionService.recordView(fixture.informationId(), null);
            assertEquals(1, first.viewCount());
            assertEquals(2, second.viewCount());
            assertNotNull(second.lastViewedAt());

            UserInformationInteraction interested = interactionService.replaceFeedback(
                    fixture.informationId(), "INTERESTED", null);
            assertEquals(FeedbackState.INTERESTED, interested.feedbackState());
            assertNotNull(interested.feedbackUpdatedAt());

            UserInformationInteraction contacted = interactionService.replaceJobDisposition(
                    fixture.informationId(), "CONTACTED", null);
            assertEquals(FeedbackState.INTERESTED, contacted.feedbackState());
            assertEquals(JobDisposition.CONTACTED, contacted.jobDisposition());

            UserInformationInteraction unsuitable = interactionService.replaceJobDisposition(
                    fixture.informationId(), "CONTACTED_NOT_SUITABLE", null);
            assertEquals(FeedbackState.INTERESTED, unsuitable.feedbackState());
            assertEquals(
                    JobDisposition.CONTACTED_NOT_SUITABLE,
                    unsuitable.jobDisposition());
            assertNotNull(unsuitable.dispositionUpdatedAt());

            UserInformationInteraction feedbackCleared = interactionService.replaceFeedback(
                    fixture.informationId(), "NONE", null);
            assertEquals(FeedbackState.NONE, feedbackCleared.feedbackState());
            assertEquals(
                    JobDisposition.CONTACTED_NOT_SUITABLE,
                    feedbackCleared.jobDisposition());

            UserInformationInteraction dispositionCleared =
                    interactionService.replaceJobDisposition(
                            fixture.informationId(), "NONE", null);
            assertEquals(FeedbackState.NONE, dispositionCleared.feedbackState());
            assertEquals(JobDisposition.NONE, dispositionCleared.jobDisposition());
            assertNull(dispositionCleared.lastRecommendationItemId());
        } finally {
            deleteFixture(fixture);
        }
    }

    @Test
    void concurrentFirstViewsCreateOneAggregateAndLoseNoCounts() throws Exception {
        TestOwnerInformation fixture = createFixture("concurrent");
        int requests = 8;
        ExecutorService executor = Executors.newFixedThreadPool(requests);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int index = 0; index < requests; index++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    interactionService.recordView(fixture.informationId(), null);
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get();
            }

            List<UserInformationInteractionPo> interactions = interactionMapper.selectList(
                    Wrappers.<UserInformationInteractionPo>lambdaQuery()
                            .eq(UserInformationInteractionPo::getUserId, fixture.userId())
                            .eq(UserInformationInteractionPo::getInformationId,
                                    fixture.informationId()));
            assertEquals(1, interactions.size());
            assertEquals(requests, interactions.getFirst().getViewCount());
        } finally {
            executor.shutdownNow();
            deleteFixture(fixture);
        }
    }

    private TestOwnerInformation createFixture(String prefix) {
        UserAccountPo user = new UserAccountPo();
        user.setUsername("task036-" + prefix + "-" + UUID.randomUUID());
        user.setPasswordHash("$2a$12$task036-not-a-real-secret");
        user.setTimezone("Asia/Shanghai");
        assertEquals(1, userAccountMapper.insert(user));
        when(currentUserProvider.requireCurrentUser()).thenReturn(new AuthenticatedUser(
                user.getId(), user.getUsername(), null, user.getTimezone()));

        LocalDateTime now = LocalDateTime.now();
        InformationItemPo information = new InformationItemPo();
        information.setInformationType("JOB");
        information.setSource("TASK036_TEST");
        information.setSourceItemId(UUID.randomUUID().toString());
        information.setTitle("TASK-036 test job");
        information.setCollectedAt(now);
        information.setFirstSeenTime(now);
        information.setLastSeenTime(now);
        information.setContentHash("a".repeat(64));
        information.setCurrentVersionNo(1);
        information.setRawPayload("{}");
        information.setSchemaVersion(1);
        information.setCollectorId("task036-test");
        information.setCollectorVersion("1");
        assertEquals(1, informationItemMapper.insert(information));
        return new TestOwnerInformation(user.getId(), information.getId());
    }

    private void deleteFixture(TestOwnerInformation fixture) {
        UserInformationInteractionPo interaction =
                interactionMapper.selectOwnedInteraction(fixture.userId(), fixture.informationId());
        if (interaction != null) {
            jobDispositionMapper.deleteById(interaction.getId());
            interactionMapper.deleteById(interaction.getId());
        }
        informationItemMapper.deleteById(fixture.informationId());
        userAccountMapper.deleteById(fixture.userId());
    }

    private record TestOwnerInformation(
            /** 测试 Owner 主键。 */
            long userId,
            /** 测试 JOB Information 主键。 */
            long informationId) {
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }
}
