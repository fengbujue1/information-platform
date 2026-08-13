package com.informationplatform.hub.analysis.preview.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.informationplatform.hub.analysis.candidate.application.CandidateResolverRegistry;
import com.informationplatform.hub.analysis.candidate.domain.AnalysisCandidate;
import com.informationplatform.hub.analysis.candidate.domain.CandidateResolution;
import com.informationplatform.hub.analysis.candidate.domain.CandidateResolutionRequest;
import com.informationplatform.hub.analysis.candidate.domain.CandidateResolver;
import com.informationplatform.hub.analysis.definition.application.AnalysisDefinitionRegistry;
import com.informationplatform.hub.analysis.definition.domain.AnalysisDefinition;
import com.informationplatform.hub.analysis.definition.domain.AnalysisDefinitionId;
import com.informationplatform.hub.analysis.definition.domain.AnalysisInformationType;
import com.informationplatform.hub.analysis.definition.domain.AnalysisSnapshotSource;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiPromptProfileMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiPromptVersionMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiPromptProfilePo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiPromptVersionPo;
import com.informationplatform.hub.analysis.processing.application.AnalysisPromptAssembler;
import com.informationplatform.hub.analysis.processing.application.AnalysisTokenEstimator;
import com.informationplatform.hub.analysis.processing.domain.AnalysisTokenEstimate;
import com.informationplatform.hub.analysis.processing.domain.AssembledAnalysisPrompt;
import com.informationplatform.hub.analysis.preview.domain.AnalysisPreview;
import com.informationplatform.hub.analysis.preview.domain.PreviewTokenPayload;
import com.informationplatform.hub.analysis.preview.infrastructure.config.AnalysisPreviewProperties;
import com.informationplatform.hub.analysis.provider.domain.AiProviderMessage;
import com.informationplatform.hub.analysis.provider.domain.AiProviderMessageRole;
import com.informationplatform.hub.analysis.provider.domain.AiProviderRequest;
import com.informationplatform.hub.identity.application.CurrentUserProvider;
import com.informationplatform.hub.identity.domain.AuthenticatedUser;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

class AnalysisPreviewServiceTest {

    @Test
    void previewUsesReadOnlyRepeatableReadTransaction() throws Exception {
        Transactional transaction = AnalysisPreviewService.class
                .getMethod(
                        "preview",
                        long.class,
                        Integer.class,
                        Integer.class,
                        Long.class)
                .getAnnotation(Transactional.class);

        assertThat(transaction).isNotNull();
        assertThat(transaction.readOnly()).isTrue();
        assertThat(transaction.isolation()).isEqualTo(Isolation.REPEATABLE_READ);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void freezesWindowAppliesLimitsAndIssuesReusableToken() {
        Instant now = Instant.parse("2026-08-05T06:00:00.123456Z");
        CurrentUserProvider users = mock(CurrentUserProvider.class);
        AiPromptProfileMapper profiles = mock(AiPromptProfileMapper.class);
        AiPromptVersionMapper versions = mock(AiPromptVersionMapper.class);
        AnalysisDefinitionRegistry definitions = mock(AnalysisDefinitionRegistry.class);
        CandidateResolverRegistry resolvers = mock(CandidateResolverRegistry.class);
        AnalysisPromptAssembler assembler = mock(AnalysisPromptAssembler.class);
        AnalysisTokenEstimator estimator = mock(AnalysisTokenEstimator.class);
        CandidateFingerprintCalculator fingerprints = mock(CandidateFingerprintCalculator.class);
        PreviewTokenService tokens = mock(PreviewTokenService.class);
        CandidateResolver resolver = mock(CandidateResolver.class);
        AnalysisDefinition definition = mock(AnalysisDefinition.class);

        AiPromptProfilePo profile = profile();
        AiPromptVersionPo version = version();
        when(users.requireCurrentUser())
                .thenReturn(new AuthenticatedUser(7, "owner", "Owner", "Asia/Shanghai"));
        when(profiles.selectOwnedById(11, 7)).thenReturn(profile);
        when(versions.selectOne(any())).thenReturn(version);
        when(definitions.requireCurrent("JOB_USER_RELEVANCE")).thenReturn(definition);
        when(definition.id()).thenReturn(new AnalysisDefinitionId("JOB_USER_RELEVANCE", 1));
        when(definition.informationType()).thenReturn(AnalysisInformationType.JOB);
        when(resolvers.require(AnalysisInformationType.JOB)).thenReturn(resolver);
        List<AnalysisCandidate> candidates = List.of(candidate(101), candidate(102));
        when(resolver.resolve(any())).thenReturn(new CandidateResolution(4, 1, 3, candidates));
        AiProviderRequest request = new AiProviderRequest(
                List.of(new AiProviderMessage(AiProviderMessageRole.USER, "candidate")), 1000);
        AssembledAnalysisPrompt assembled = mock(AssembledAnalysisPrompt.class);
        when(assembler.assemble(any(), any(), any())).thenReturn(assembled);
        when(assembled.providerRequest()).thenReturn(request);
        when(estimator.estimate(request))
                .thenReturn(new AnalysisTokenEstimate(100, 1000, 1100,
                        AnalysisTokenEstimator.METHOD));
        when(fingerprints.calculate(any())).thenReturn("f".repeat(64));
        when(tokens.issue(any())).thenReturn("signed-token");

        AnalysisPreviewService service = new AnalysisPreviewService(
                users, profiles, versions, definitions, resolvers, assembler, estimator,
                fingerprints, tokens, limitPolicy(100), Clock.fixed(now, ZoneOffset.UTC));
        AnalysisPreview preview = service.preview(11, 3, 80, 1500L);

        assertThat(preview.windowEnd()).isEqualTo(Instant.parse("2026-08-05T06:00:00.123Z"));
        assertThat(preview.windowStart()).isEqualTo(Instant.parse("2026-08-02T06:00:00.123Z"));
        assertThat(preview.totalInWindow()).isEqualTo(4);
        assertThat(preview.alreadyAnalyzedCount()).isEqualTo(1);
        assertThat(preview.eligibleCount()).isEqualTo(3);
        assertThat(preview.selectedCount()).isEqualTo(1);
        assertThat(preview.deferredByItemLimitCount()).isEqualTo(1);
        assertThat(preview.deferredByTokenBudgetCount()).isEqualTo(1);
        assertThat(preview.estimatedTotalTokens()).isEqualTo(1100);
        assertThat(preview.previewToken()).isEqualTo("signed-token");

        ArgumentCaptor<CandidateResolutionRequest> requestCaptor =
                ArgumentCaptor.forClass(CandidateResolutionRequest.class);
        org.mockito.Mockito.verify(resolver).resolve(requestCaptor.capture());
        assertThat(requestCaptor.getValue().windowStart())
                .isEqualTo(LocalDateTime.ofInstant(preview.windowStart(), ZoneOffset.UTC));
        assertThat(requestCaptor.getValue().maxCandidates()).isEqualTo(80);
        ArgumentCaptor<PreviewTokenPayload> tokenCaptor =
                ArgumentCaptor.forClass(PreviewTokenPayload.class);
        org.mockito.Mockito.verify(tokens).issue(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().manualRequestId()).isNotBlank();
        assertThat(tokenCaptor.getValue().expiresAt())
                .isEqualTo(preview.windowEnd().plusSeconds(600));
    }

    @Test
    void rejectsFrozenTokenThatExceedsCurrentConfiguredLimits() {
        CurrentUserProvider users = mock(CurrentUserProvider.class);
        when(users.requireCurrentUser())
                .thenReturn(new AuthenticatedUser(7, "owner", "Owner", "Asia/Shanghai"));
        CandidateResolverRegistry resolvers = mock(CandidateResolverRegistry.class);
        AnalysisPreviewProperties properties = new AnalysisPreviewProperties();
        properties.setMaxCandidates(40);
        properties.afterPropertiesSet();
        AnalysisPreviewService service = new AnalysisPreviewService(
                users,
                mock(AiPromptProfileMapper.class),
                mock(AiPromptVersionMapper.class),
                mock(AnalysisDefinitionRegistry.class),
                resolvers,
                mock(AnalysisPromptAssembler.class),
                mock(AnalysisTokenEstimator.class),
                mock(CandidateFingerprintCalculator.class),
                mock(PreviewTokenService.class),
                new AnalysisPreviewLimitPolicy(properties),
                Clock.systemUTC());
        PreviewTokenPayload payload = mock(PreviewTokenPayload.class);
        when(payload.userId()).thenReturn(7L);
        when(payload.windowDays()).thenReturn(3);
        when(payload.maxCandidates()).thenReturn(50);
        when(payload.maxEstimatedTokens()).thenReturn(75_000L);

        assertThatThrownBy(() -> service.recompute(payload))
                .isInstanceOf(AnalysisPreviewConflictException.class)
                .hasMessage("Preview limits no longer satisfy the current platform policy");
        verifyNoInteractions(resolvers);
    }

    @Test
    void rejectsUnknownOrCrossOwnerProfileBeforeResolvingCandidates() {
        CurrentUserProvider users = mock(CurrentUserProvider.class);
        AiPromptProfileMapper profiles = mock(AiPromptProfileMapper.class);
        AiPromptVersionMapper versions = mock(AiPromptVersionMapper.class);
        AnalysisDefinitionRegistry definitions = mock(AnalysisDefinitionRegistry.class);
        CandidateResolverRegistry resolvers = mock(CandidateResolverRegistry.class);
        AnalysisPromptAssembler assembler = mock(AnalysisPromptAssembler.class);
        AnalysisTokenEstimator estimator = mock(AnalysisTokenEstimator.class);
        CandidateFingerprintCalculator fingerprints = mock(CandidateFingerprintCalculator.class);
        PreviewTokenService tokens = mock(PreviewTokenService.class);
        when(users.requireCurrentUser())
                .thenReturn(new AuthenticatedUser(7, "owner", "Owner", "Asia/Shanghai"));

        AnalysisPreviewService service = new AnalysisPreviewService(
                users,
                profiles,
                versions,
                definitions,
                resolvers,
                assembler,
                estimator,
                fingerprints,
                tokens,
                limitPolicy(),
                Clock.systemUTC());

        assertThatThrownBy(() -> service.preview(99, 3, 20, 75_000L))
                .isInstanceOf(AnalysisPreviewNotFoundException.class)
                .hasMessage("Prompt Profile does not exist");
        verifyNoInteractions(versions, definitions, resolvers, assembler, estimator, tokens);
    }

    private AnalysisPreviewLimitPolicy limitPolicy() {
        return limitPolicy(50);
    }

    private AnalysisPreviewLimitPolicy limitPolicy(int maxCandidates) {
        AnalysisPreviewProperties properties = new AnalysisPreviewProperties();
        properties.setMaxCandidates(maxCandidates);
        properties.afterPropertiesSet();
        return new AnalysisPreviewLimitPolicy(properties);
    }

    private AiPromptProfilePo profile() {
        AiPromptProfilePo profile = new AiPromptProfilePo();
        profile.setId(11L);
        profile.setUserId(7L);
        profile.setAnalysisDefinitionKey("JOB_USER_RELEVANCE");
        profile.setActiveVersionId(12L);
        profile.setStatus("ACTIVE");
        return profile;
    }

    private AiPromptVersionPo version() {
        AiPromptVersionPo version = new AiPromptVersionPo();
        version.setId(12L);
        version.setPromptProfileId(11L);
        version.setVersionNo(1);
        version.setContent("Java");
        version.setContentHash("a".repeat(64));
        version.setCreatedAt(LocalDateTime.of(2026, 8, 1, 0, 0));
        return version;
    }

    private AnalysisCandidate candidate(long id) {
        AnalysisSnapshotSource source = new AnalysisSnapshotSource(
                id + 1000, id, AnalysisInformationType.JOB,
                "title", "content", new com.fasterxml.jackson.databind.ObjectMapper()
                        .createObjectNode());
        return new AnalysisCandidate(
                id, id + 1000, LocalDateTime.of(2026, 8, 5, 1, 0), source);
    }
}
