package com.informationplatform.hub.analysis.schedule.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiAnalysisBatchMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiAnalysisScheduleMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiPromptProfileMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiPromptVersionMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiAnalysisBatchPo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiAnalysisSchedulePo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiPromptProfilePo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiPromptVersionPo;
import com.informationplatform.hub.identity.infrastructure.persistence.mapper.UserAccountMapper;
import com.informationplatform.hub.identity.infrastructure.persistence.po.UserAccountPo;
import com.informationplatform.hub.testing.DatabaseIntegrationTestSafety;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

/** 在真实 MySQL 验证 due-row 查询和 Scheduled Batch 计划点唯一键。 */
@SpringBootTest(properties = "information-hub.ai.schedule.dispatcher-enabled=false")
@EnabledIfEnvironmentVariable(
        named = "INFORMATION_HUB_TEST_DB_URL",
        matches = "jdbc:mysql://.+")
class AnalysisSchedulePersistenceIntegrationTest {

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        String databaseUrl = DatabaseIntegrationTestSafety.requireTestDatabase(
                required("INFORMATION_HUB_TEST_DB_URL"));
        registry.add("spring.datasource.url", () -> databaseUrl);
        registry.add(
                "spring.datasource.username",
                () -> required("INFORMATION_HUB_TEST_DB_USERNAME"));
        registry.add(
                "spring.datasource.password",
                () -> required("INFORMATION_HUB_TEST_DB_PASSWORD"));
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired
    private UserAccountMapper userMapper;

    @Autowired
    private AiPromptProfileMapper profileMapper;

    @Autowired
    private AiPromptVersionMapper versionMapper;

    @Autowired
    private AiAnalysisScheduleMapper scheduleMapper;

    @Autowired
    private AiAnalysisBatchMapper batchMapper;

    @Test
    @Transactional
    void locksDueScheduleAndRejectsDuplicateScheduledFor() {
        UserAccountPo user = new UserAccountPo();
        user.setUsername("task030-" + UUID.randomUUID());
        user.setPasswordHash("$2a$12$task030-not-a-real-secret");
        user.setTimezone("Asia/Shanghai");
        assertThat(userMapper.insert(user)).isEqualTo(1);

        AiPromptProfilePo profile = new AiPromptProfilePo();
        profile.setUserId(user.getId());
        profile.setName("TASK-030 Profile");
        profile.setAnalysisDefinitionKey("JOB_USER_RELEVANCE");
        assertThat(profileMapper.insert(profile)).isEqualTo(1);

        AiPromptVersionPo version = new AiPromptVersionPo();
        version.setPromptProfileId(profile.getId());
        version.setVersionNo(1);
        version.setContent("关注 Java 后端岗位");
        version.setContentHash("b".repeat(64));
        assertThat(versionMapper.insert(version)).isEqualTo(1);
        profile.setActiveVersionId(version.getId());
        assertThat(profileMapper.updateById(profile)).isEqualTo(1);

        LocalDateTime scheduledFor = LocalDateTime.of(2000, 1, 1, 0, 0);
        AiAnalysisSchedulePo schedule = new AiAnalysisSchedulePo();
        schedule.setUserId(user.getId());
        schedule.setName("TASK-030 Daily");
        schedule.setPromptProfileId(profile.getId());
        schedule.setEnabled(true);
        schedule.setLocalTime(LocalTime.of(2, 0));
        schedule.setTimezone("Asia/Shanghai");
        schedule.setWindowDays(3);
        schedule.setMaxCandidates(20);
        schedule.setMaxEstimatedTokens(75_000L);
        schedule.setNextRunAt(scheduledFor);
        assertThat(scheduleMapper.insert(schedule)).isEqualTo(1);

        AiAnalysisSchedulePo due =
                scheduleMapper.selectNextDueForUpdate(LocalDateTime.of(2000, 1, 1, 0, 1));
        assertThat(due.getId()).isEqualTo(schedule.getId());
        assertThat(scheduleMapper.updateStatusAndNextRun(
                        schedule.getId(), user.getId(), false, null))
                .isEqualTo(1);
        AiAnalysisSchedulePo disabled = scheduleMapper.selectById(schedule.getId());
        assertThat(disabled.getEnabled()).isFalse();
        assertThat(disabled.getNextRunAt()).isNull();

        AiAnalysisBatchPo batch =
                scheduledBatch(user.getId(), profile.getId(), version.getId(),
                        schedule.getId(), scheduledFor);
        assertThat(batchMapper.insert(batch)).isEqualTo(1);
        assertThat(batchMapper.selectScheduled(schedule.getId(), scheduledFor).getId())
                .isEqualTo(batch.getId());
        assertThat(batchMapper.selectActiveScheduled(schedule.getId()).getId())
                .isEqualTo(batch.getId());

        AiAnalysisBatchPo duplicate =
                scheduledBatch(user.getId(), profile.getId(), version.getId(),
                        schedule.getId(), scheduledFor);
        assertThatThrownBy(() -> batchMapper.insert(duplicate))
                .isInstanceOf(DuplicateKeyException.class);
    }

    private AiAnalysisBatchPo scheduledBatch(
            long userId,
            long profileId,
            long versionId,
            long scheduleId,
            LocalDateTime scheduledFor) {
        AiAnalysisBatchPo batch = new AiAnalysisBatchPo();
        batch.setUserId(userId);
        batch.setTriggerType("SCHEDULED");
        batch.setScheduleId(scheduleId);
        batch.setScheduledFor(scheduledFor);
        batch.setInformationType("JOB");
        batch.setAnalysisDefinitionKey("JOB_USER_RELEVANCE");
        batch.setAnalysisDefinitionVersion(1);
        batch.setPromptProfileId(profileId);
        batch.setPromptVersionId(versionId);
        batch.setRequestedWindowDays(3);
        batch.setWindowStart(scheduledFor.minusDays(3));
        batch.setWindowEnd(scheduledFor);
        batch.setRequestedMaxCandidates(20);
        batch.setRequestedTokenBudget(75_000L);
        batch.setStatus("PENDING");
        return batch;
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }
}
