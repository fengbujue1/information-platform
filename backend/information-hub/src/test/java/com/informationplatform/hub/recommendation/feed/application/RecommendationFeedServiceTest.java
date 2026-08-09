package com.informationplatform.hub.recommendation.feed.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.informationplatform.hub.identity.application.CurrentUserProvider;
import com.informationplatform.hub.identity.domain.AuthenticatedUser;
import com.informationplatform.hub.recommendation.feed.infrastructure.persistence.RecommendationFeedQueryMapper;
import com.informationplatform.hub.recommendation.feed.infrastructure.persistence.RecommendationFeedQueryRow;
import com.informationplatform.hub.recommendation.infrastructure.persistence.mapper.RecommendationRunMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.mapper.UserRecommendationProfileMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.po.RecommendationRunPo;
import com.informationplatform.hub.recommendation.infrastructure.persistence.po.UserRecommendationProfilePo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 验证 Feed 默认分页、空结果、Profile stale 和 current Interaction 投影。 */
class RecommendationFeedServiceTest {

    @Test
    void returnsEmptyFeedWithoutSuccessfulRunAndDoesNotCalculate() {
        Fixture fixture = fixture();

        var feed = fixture.service.get("JOB", null, null);

        assertThat(feed.run()).isNull();
        assertThat(feed.page()).isEqualTo(1);
        assertThat(feed.pageSize()).isEqualTo(20);
        assertThat(feed.total()).isZero();
        assertThat(feed.items()).isEmpty();
        verify(fixture.runs).selectLatestCompleted(7, "JOB");
        verifyNoInteractions(fixture.profiles, fixture.feedQueries);
    }

    @Test
    void returnsVisiblePageAndMarksChangedProfile() {
        Fixture fixture = fixture();
        RecommendationRunPo run = completedRun();
        UserRecommendationProfilePo profile = new UserRecommendationProfilePo();
        profile.setContentHash("b".repeat(64));
        RecommendationFeedQueryRow row = row();
        when(fixture.runs.selectLatestCompleted(7, "JOB")).thenReturn(run);
        when(fixture.profiles.selectOwnedByType(7, "JOB")).thenReturn(profile);
        when(fixture.feedQueries.countVisible(301, 7)).thenReturn(21L);
        when(fixture.feedQueries.selectVisible(301, 7, 10, 10)).thenReturn(List.of(row));

        var feed = fixture.service.get("JOB", 2, 10);

        assertThat(feed.run().profileChangedSinceRun()).isTrue();
        assertThat(feed.total()).isEqualTo(21);
        assertThat(feed.items()).singleElement().satisfies(item -> {
            assertThat(item.feedbackState()).isEqualTo("INTERESTED");
            assertThat(item.jobDisposition()).isEqualTo("CONTACTED");
            assertThat(item.viewed()).isTrue();
            assertThat(item.reasons()).containsExactly("AI 相关度高", "最近进入平台");
            assertThat(item.job().title()).isEqualTo("Java Backend Engineer");
        });
    }

    @Test
    void rejectsUnsupportedTypeAndInvalidPagination() {
        Fixture fixture = fixture();

        assertThatThrownBy(() -> fixture.service.get("NEWS", 1, 20))
                .isInstanceOf(RecommendationFeedRequestException.class)
                .extracting("code")
                .isEqualTo("RECOMMENDATION_INFORMATION_TYPE_UNSUPPORTED");
        assertThatThrownBy(() -> fixture.service.get("JOB", 0, 20))
                .isInstanceOf(RecommendationFeedRequestException.class)
                .extracting("code")
                .isEqualTo("RECOMMENDATION_FEED_PAGE_INVALID");
        assertThatThrownBy(() -> fixture.service.get("JOB", 1, 101))
                .isInstanceOf(RecommendationFeedRequestException.class)
                .extracting("code")
                .isEqualTo("RECOMMENDATION_FEED_PAGE_SIZE_INVALID");
    }

    @Test
    void rejectsMalformedPersistedReasons() {
        Fixture fixture = fixture();
        RecommendationFeedQueryRow row = row();
        row.setReasonsJson("not-json");
        when(fixture.runs.selectLatestCompleted(7, "JOB")).thenReturn(completedRun());
        when(fixture.profiles.selectOwnedByType(7, "JOB")).thenReturn(profile("a"));
        when(fixture.feedQueries.countVisible(301, 7)).thenReturn(1L);
        when(fixture.feedQueries.selectVisible(301, 7, 20, 0)).thenReturn(List.of(row));

        assertThatThrownBy(() -> fixture.service.get("JOB", null, null))
                .isInstanceOf(RecommendationFeedPersistenceException.class)
                .hasMessageContaining("reasons");
    }

    private Fixture fixture() {
        CurrentUserProvider users = mock(CurrentUserProvider.class);
        RecommendationRunMapper runs = mock(RecommendationRunMapper.class);
        UserRecommendationProfileMapper profiles = mock(UserRecommendationProfileMapper.class);
        RecommendationFeedQueryMapper feedQueries = mock(RecommendationFeedQueryMapper.class);
        when(users.requireCurrentUser()).thenReturn(
                new AuthenticatedUser(7, "task042", null, "Asia/Shanghai"));
        return new Fixture(
                runs,
                profiles,
                feedQueries,
                new RecommendationFeedService(
                        users, runs, profiles, feedQueries, new ObjectMapper()));
    }

    private RecommendationRunPo completedRun() {
        RecommendationRunPo run = new RecommendationRunPo();
        run.setId(301L);
        run.setInformationType("JOB");
        run.setTriggerType("MANUAL");
        run.setProfileContentHash("a".repeat(64));
        run.setAlgorithmKey("JOB_RECOMMENDATION");
        run.setAlgorithmVersion(1);
        run.setCompletedAt(LocalDateTime.of(2026, 8, 9, 18, 0));
        return run;
    }

    private UserRecommendationProfilePo profile(String hashPrefix) {
        UserRecommendationProfilePo profile = new UserRecommendationProfilePo();
        profile.setContentHash(hashPrefix.repeat(64));
        return profile;
    }

    private RecommendationFeedQueryRow row() {
        RecommendationFeedQueryRow row = new RecommendationFeedQueryRow();
        row.setRecommendationItemId(1001L);
        row.setInformationId(88L);
        row.setSnapshotId(102L);
        row.setRankNo(3);
        row.setFinalScore(new BigDecimal("92.400"));
        row.setAiRelevanceScore(new BigDecimal("95.000"));
        row.setProfileMatchScore(new BigDecimal("86.000"));
        row.setFreshnessScore(new BigDecimal("90.000"));
        row.setReasonsJson("[\"AI 相关度高\",\"最近进入平台\"]");
        row.setFeedbackState("INTERESTED");
        row.setJobDisposition("CONTACTED");
        row.setViewCount(2);
        row.setTitle("Java Backend Engineer");
        row.setCompanyName("Example");
        row.setSalaryText("20-35K");
        row.setLocationName("成都");
        row.setRemoteType("REMOTE");
        row.setSourceUrl("https://example.test/job/88");
        return row;
    }

    private record Fixture(
            RecommendationRunMapper runs,
            UserRecommendationProfileMapper profiles,
            RecommendationFeedQueryMapper feedQueries,
            RecommendationFeedService service) {}
}
