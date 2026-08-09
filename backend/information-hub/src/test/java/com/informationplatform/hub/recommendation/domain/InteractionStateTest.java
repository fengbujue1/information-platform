package com.informationplatform.hub.recommendation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.informationplatform.hub.recommendation.job.domain.JobDisposition;
import org.junit.jupiter.api.Test;

class InteractionStateTest {

    @Test
    void acceptsOnlyContractFeedbackStates() {
        assertThat(FeedbackState.fromValue("NONE")).isEqualTo(FeedbackState.NONE);
        assertThat(FeedbackState.fromValue("INTERESTED")).isEqualTo(FeedbackState.INTERESTED);
        assertThat(FeedbackState.fromValue("NOT_INTERESTED"))
                .isEqualTo(FeedbackState.NOT_INTERESTED);
        assertThatThrownBy(() -> FeedbackState.fromValue("DISMISSED"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported feedbackState");
    }

    @Test
    void validatesJobDispositionAndKeepsContactedVisible() {
        assertThat(JobDisposition.fromValue("NONE")).isEqualTo(JobDisposition.NONE);
        assertThat(JobDisposition.fromValue("CONTACTED").isHardExclusion()).isFalse();
        assertThat(JobDisposition.fromValue("CONTACTED_NOT_SUITABLE").isHardExclusion()).isTrue();
        assertThatThrownBy(() -> JobDisposition.fromValue("REJECTED"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported jobDisposition");
    }
}
