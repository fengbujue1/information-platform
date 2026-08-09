package com.informationplatform.hub.recommendation.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.informationplatform.hub.identity.application.CurrentUserProvider;
import com.informationplatform.hub.identity.domain.AuthenticatedUser;
import com.informationplatform.hub.information.infrastructure.persistence.mapper.InformationItemMapper;
import com.informationplatform.hub.information.infrastructure.persistence.po.InformationItemPo;
import com.informationplatform.hub.recommendation.infrastructure.persistence.mapper.RecommendationItemMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.mapper.UserInformationInteractionMapper;
import com.informationplatform.hub.recommendation.job.infrastructure.persistence.mapper.UserJobDispositionMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 验证 Interaction 应用服务的 Owner、归因、类型和状态边界。 */
class RecommendationInteractionServiceTest {

    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final InformationItemMapper informationItemMapper = mock(InformationItemMapper.class);
    private final UserInformationInteractionMapper interactionMapper =
            mock(UserInformationInteractionMapper.class);
    private final UserJobDispositionMapper jobDispositionMapper =
            mock(UserJobDispositionMapper.class);
    private final RecommendationItemMapper recommendationItemMapper =
            mock(RecommendationItemMapper.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-08-09T08:00:00Z"), ZoneOffset.UTC);
    private final RecommendationInteractionService service =
            new RecommendationInteractionService(
                    currentUserProvider,
                    informationItemMapper,
                    interactionMapper,
                    jobDispositionMapper,
                    recommendationItemMapper,
                    clock);

    @BeforeEach
    void setUp() {
        when(currentUserProvider.requireCurrentUser())
                .thenReturn(new AuthenticatedUser(11L, "owner", null, "Asia/Shanghai"));
    }

    @Test
    void rejectsCrossOwnerOrMismatchedAttributionBeforeWriting() {
        when(informationItemMapper.selectById(7L)).thenReturn(information("JOB"));
        when(recommendationItemMapper.countOwnedAttribution(101L, 7L, 11L)).thenReturn(0L);

        RecommendationInteractionRequestException exception = assertThrows(
                RecommendationInteractionRequestException.class,
                () -> service.replaceFeedback(7L, "INTERESTED", 101L));

        assertEquals("RECOMMENDATION_INTERACTION_ATTRIBUTION_INVALID", exception.getCode());
        verify(interactionMapper, never())
                .replaceFeedback(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsJobDispositionForNonJobInformation() {
        when(informationItemMapper.selectById(8L)).thenReturn(information("EDUCATION"));

        RecommendationInteractionRequestException exception = assertThrows(
                RecommendationInteractionRequestException.class,
                () -> service.replaceJobDisposition(8L, "CONTACTED", null));

        assertEquals(
                "RECOMMENDATION_JOB_DISPOSITION_INFORMATION_TYPE_INVALID",
                exception.getCode());
        verify(interactionMapper, never())
                .ensureInteraction(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsUnknownStatesBeforePersistence() {
        RecommendationInteractionRequestException feedback = assertThrows(
                RecommendationInteractionRequestException.class,
                () -> service.replaceFeedback(7L, "MAYBE", null));
        RecommendationInteractionRequestException disposition = assertThrows(
                RecommendationInteractionRequestException.class,
                () -> service.replaceJobDisposition(7L, "APPLIED", null));

        assertEquals("RECOMMENDATION_FEEDBACK_STATE_INVALID", feedback.getCode());
        assertEquals("RECOMMENDATION_JOB_DISPOSITION_INVALID", disposition.getCode());
        verify(informationItemMapper, never()).selectById(7L);
    }

    private InformationItemPo information(String type) {
        InformationItemPo information = new InformationItemPo();
        information.setId(7L);
        information.setInformationType(type);
        return information;
    }
}
