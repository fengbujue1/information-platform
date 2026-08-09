package com.informationplatform.hub.recommendation.run.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiPromptProfileMapper;
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

/** 验证 Manual Run 冻结输入和同 Owner/Type 活动 Run 冲突。 */
class RecommendationRunCreationTransactionServiceTest {

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

    private Fixture fixture() {
        UserRecommendationProfileMapper profiles = mock(UserRecommendationProfileMapper.class);
        JobRecommendationProfileMapper jobProfiles = mock(JobRecommendationProfileMapper.class);
        AiPromptProfileMapper prompts = mock(AiPromptProfileMapper.class);
        RecommendationRunMapper runs = mock(RecommendationRunMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        return new Fixture(
                profiles,
                jobProfiles,
                prompts,
                runs,
                new RecommendationRunCreationTransactionService(
                        profiles,
                        jobProfiles,
                        prompts,
                        runs,
                        new JobRecommendationProfileJsonCodec(objectMapper),
                        new JobRecommendationRunProfileSnapshotCodec(objectMapper)));
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
            RecommendationRunMapper runs,
            RecommendationRunCreationTransactionService service) {
    }
}
