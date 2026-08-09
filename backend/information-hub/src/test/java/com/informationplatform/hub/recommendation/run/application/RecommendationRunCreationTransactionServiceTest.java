package com.informationplatform.hub.recommendation.run.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiAnalysisBatchMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiPromptProfileMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiAnalysisBatchPo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiPromptProfilePo;
import com.informationplatform.hub.recommendation.infrastructure.persistence.mapper.RecommendationRunMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.mapper.UserRecommendationProfileMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.po.RecommendationRunPo;
import com.informationplatform.hub.recommendation.infrastructure.persistence.po.UserRecommendationProfilePo;
import com.informationplatform.hub.recommendation.job.infrastructure.persistence.JobRecommendationProfileJsonCodec;
import com.informationplatform.hub.recommendation.job.infrastructure.persistence.mapper.JobRecommendationProfileMapper;
import com.informationplatform.hub.recommendation.job.infrastructure.persistence.po.JobRecommendationProfilePo;
import com.informationplatform.hub.recommendation.job.run.infrastructure.JobRecommendationRunProfileSnapshotCodec;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 验证 Manual Run 冻结输入和同 Owner/Type 活动 Run 冲突。 */
class RecommendationRunCreationTransactionServiceTest {

    @Test
    void autoCreationUsesIndependentTransactionAfterBatchCommit() throws Exception {
        Transactional annotation = RecommendationRunCreationTransactionService.class
                .getMethod("createAuto", long.class)
                .getAnnotation(Transactional.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }

    @Test
    void freezesCurrentProfilePromptVersionAlgorithmAndWindow() {
        Fixture fixture = fixture();
        UserRecommendationProfilePo profile = profile();
        JobRecommendationProfilePo extension = extension();
        AiPromptProfilePo prompt = prompt();
        when(fixture.profiles.selectOwnedByTypeForUpdate(7, "JOB")).thenReturn(profile);
        when(fixture.jobProfiles.selectById(21L)).thenReturn(extension);
        when(fixture.prompts.selectOwnedById(31, 7)).thenReturn(prompt);
        when(fixture.runs.insert(any(RecommendationRunPo.class))).thenAnswer(invocation -> {
            RecommendationRunPo run = invocation.getArgument(0);
            run.setId(41L);
            return 1;
        });

        var accepted = fixture.service.createManual(7, "JOB");

        assertThat(accepted.runId()).isEqualTo(41);
        ArgumentCaptor<RecommendationRunPo> captor =
                ArgumentCaptor.forClass(RecommendationRunPo.class);
        verify(fixture.runs).insert(captor.capture());
        RecommendationRunPo run = captor.getValue();
        assertThat(run.getStatus()).isEqualTo("PENDING");
        assertThat(run.getPromptVersionId()).isEqualTo(32);
        assertThat(run.getAlgorithmKey()).isEqualTo("JOB_RECOMMENDATION");
        assertThat(run.getWindowStart()).isEqualTo(run.getWindowEnd().minusDays(7));
        assertThat(run.getProfileSnapshotJson()).contains(
                "\"informationType\":\"JOB\"", "\"targetRoles\":[\"Java 后端\"]");
    }

    @Test
    void rejectsSecondActiveManualRun() {
        Fixture fixture = fixture();
        when(fixture.profiles.selectOwnedByTypeForUpdate(7, "JOB")).thenReturn(profile());
        when(fixture.jobProfiles.selectById(21L)).thenReturn(extension());
        when(fixture.prompts.selectOwnedById(31, 7)).thenReturn(prompt());
        when(fixture.runs.selectActiveManualForUpdate(7, "JOB"))
                .thenReturn(new RecommendationRunPo());

        assertThatThrownBy(() -> fixture.service.createManual(7, "JOB"))
                .isInstanceOf(RecommendationRunConflictException.class)
                .extracting("code")
                .isEqualTo("RECOMMENDATION_RUN_IN_PROGRESS");
    }

    @Test
    void createsAutoRunForEligibleManualAndScheduledBatchesUsingFrozenPromptVersion() {
        for (String triggerType : List.of("MANUAL", "SCHEDULED")) {
            Fixture fixture = fixture();
            AiAnalysisBatchPo batch = batch("COMPLETED", triggerType);
            when(fixture.batches.selectById(61L)).thenReturn(batch);
            when(fixture.profiles.selectOwnedByTypeForUpdate(7, "JOB"))
                    .thenReturn(profile());
            when(fixture.jobProfiles.selectById(21L)).thenReturn(extension());
            when(fixture.runs.insert(any(RecommendationRunPo.class))).thenAnswer(invocation -> {
                RecommendationRunPo run = invocation.getArgument(0);
                run.setId(71L);
                return 1;
            });

            var outcome = fixture.service.createAuto(61L);

            assertThat(outcome.created()).isTrue();
            ArgumentCaptor<RecommendationRunPo> captor =
                    ArgumentCaptor.forClass(RecommendationRunPo.class);
            verify(fixture.runs).insert(captor.capture());
            RecommendationRunPo run = captor.getValue();
            assertThat(run.getTriggerType()).isEqualTo("ANALYSIS_BATCH_COMPLETED");
            assertThat(run.getSourceAnalysisBatchId()).isEqualTo(61);
            assertThat(run.getPromptVersionId()).isEqualTo(44);
            assertThat(run.getStatus()).isEqualTo("PENDING");
        }
    }

    @Test
    void createsAutoRunForPartialFailedBatch() {
        Fixture fixture = eligibleAutoFixture("PARTIAL_FAILED", "SCHEDULED");

        assertThat(fixture.service.createAuto(61).created()).isTrue();
    }

    @Test
    void skipsFailedNoopMissingProfileAndPromptMismatch() {
        Fixture failed = fixture();
        when(failed.batches.selectById(61L)).thenReturn(batch("FAILED", "MANUAL"));
        assertThat(failed.service.createAuto(61).skipReason())
                .isEqualTo("SOURCE_BATCH_STATUS_FAILED");

        Fixture noop = fixture();
        when(noop.batches.selectById(61L)).thenReturn(batch("NOOP", "SCHEDULED"));
        assertThat(noop.service.createAuto(61).skipReason())
                .isEqualTo("SOURCE_BATCH_STATUS_NOOP");

        Fixture missing = fixture();
        when(missing.batches.selectById(61L)).thenReturn(batch("COMPLETED", "MANUAL"));
        assertThat(missing.service.createAuto(61).skipReason())
                .isEqualTo("RECOMMENDATION_PROFILE_NOT_FOUND");

        Fixture mismatch = fixture();
        when(mismatch.batches.selectById(61L)).thenReturn(batch("COMPLETED", "MANUAL"));
        UserRecommendationProfilePo mismatched = profile();
        mismatched.setAnalysisPromptProfileId(99L);
        when(mismatch.profiles.selectOwnedByTypeForUpdate(7, "JOB"))
                .thenReturn(mismatched);
        assertThat(mismatch.service.createAuto(61).skipReason())
                .isEqualTo("PROMPT_PROFILE_MISMATCH");
    }

    @Test
    void treatsDuplicateSourceBatchAsIdempotent() {
        Fixture fixture = fixture();
        when(fixture.batches.selectById(61L)).thenReturn(batch("COMPLETED", "MANUAL"));
        RecommendationRunPo existing = new RecommendationRunPo();
        existing.setId(71L);
        when(fixture.runs.selectBySourceAnalysisBatchId(61L)).thenReturn(existing);

        var outcome = fixture.service.createAuto(61);

        assertThat(outcome.created()).isFalse();
        assertThat(outcome.runId()).isEqualTo(71);
        assertThat(outcome.skipReason()).isEqualTo("SOURCE_BATCH_ALREADY_TRIGGERED");
    }

    private Fixture fixture() {
        UserRecommendationProfileMapper profiles = mock(UserRecommendationProfileMapper.class);
        JobRecommendationProfileMapper jobProfiles = mock(JobRecommendationProfileMapper.class);
        AiPromptProfileMapper prompts = mock(AiPromptProfileMapper.class);
        AiAnalysisBatchMapper batches = mock(AiAnalysisBatchMapper.class);
        RecommendationRunMapper runs = mock(RecommendationRunMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        return new Fixture(
                profiles,
                jobProfiles,
                prompts,
                batches,
                runs,
                new RecommendationRunCreationTransactionService(
                        profiles,
                        jobProfiles,
                        prompts,
                        batches,
                        runs,
                        new JobRecommendationProfileJsonCodec(objectMapper),
                        new JobRecommendationRunProfileSnapshotCodec(objectMapper)));
    }

    private Fixture eligibleAutoFixture(String status, String triggerType) {
        Fixture fixture = fixture();
        when(fixture.batches.selectById(61L)).thenReturn(batch(status, triggerType));
        when(fixture.profiles.selectOwnedByTypeForUpdate(7, "JOB")).thenReturn(profile());
        when(fixture.jobProfiles.selectById(21L)).thenReturn(extension());
        when(fixture.runs.insert(any(RecommendationRunPo.class))).thenAnswer(invocation -> {
            RecommendationRunPo run = invocation.getArgument(0);
            run.setId(71L);
            return 1;
        });
        return fixture;
    }

    private AiAnalysisBatchPo batch(String status, String triggerType) {
        AiAnalysisBatchPo batch = new AiAnalysisBatchPo();
        batch.setId(61L);
        batch.setUserId(7L);
        batch.setInformationType("JOB");
        batch.setTriggerType(triggerType);
        batch.setPromptProfileId(31L);
        batch.setPromptVersionId(44L);
        batch.setStatus(status);
        return batch;
    }

    private UserRecommendationProfilePo profile() {
        UserRecommendationProfilePo profile = new UserRecommendationProfilePo();
        profile.setId(21L);
        profile.setUserId(7L);
        profile.setInformationType("JOB");
        profile.setAnalysisPromptProfileId(31L);
        profile.setWindowDays(7);
        profile.setTopN(50);
        profile.setContentHash("a".repeat(64));
        return profile;
    }

    private JobRecommendationProfilePo extension() {
        JobRecommendationProfilePo extension = new JobRecommendationProfilePo();
        extension.setProfileId(21L);
        extension.setTargetRoles("[\"Java 后端\"]");
        extension.setPreferredSkills("[\"Java\"]");
        extension.setPreferredCities("[\"上海\"]");
        extension.setPreferredRemoteTypes("[\"REMOTE\"]");
        extension.setSalaryMinMonthlyYuan(20_000);
        extension.setExcludedKeywords("[]");
        return extension;
    }

    private AiPromptProfilePo prompt() {
        AiPromptProfilePo prompt = new AiPromptProfilePo();
        prompt.setId(31L);
        prompt.setUserId(7L);
        prompt.setAnalysisDefinitionKey("JOB_USER_RELEVANCE");
        prompt.setActiveVersionId(32L);
        prompt.setStatus("ACTIVE");
        return prompt;
    }

    private record Fixture(
            UserRecommendationProfileMapper profiles,
            JobRecommendationProfileMapper jobProfiles,
            AiPromptProfileMapper prompts,
            AiAnalysisBatchMapper batches,
            RecommendationRunMapper runs,
            RecommendationRunCreationTransactionService service) {
    }
}
