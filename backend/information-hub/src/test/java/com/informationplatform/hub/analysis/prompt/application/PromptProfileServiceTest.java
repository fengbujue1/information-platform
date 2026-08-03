package com.informationplatform.hub.analysis.prompt.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiPromptProfileMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiPromptVersionMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiPromptProfilePo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiPromptVersionPo;
import com.informationplatform.hub.analysis.prompt.domain.PromptContentHasher;
import com.informationplatform.hub.analysis.prompt.domain.PromptVersionChange;
import com.informationplatform.hub.identity.application.CurrentUserProvider;
import com.informationplatform.hub.identity.domain.AuthenticatedUser;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

/** 验证 Prompt 版本不可变、Owner 隔离、复用和事务编排所需调用。 */
@ExtendWith(MockitoExtension.class)
class PromptProfileServiceTest {

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private AiPromptProfileMapper profileMapper;

    @Mock
    private AiPromptVersionMapper versionMapper;

    private PromptProfileService service;

    @BeforeEach
    void setUp() {
        when(currentUserProvider.requireCurrentUser())
                .thenReturn(new AuthenticatedUser(7L, "owner", "Owner", "Asia/Shanghai"));
        service = new PromptProfileService(
                currentUserProvider,
                profileMapper,
                versionMapper,
                new PromptContentHasher());
    }

    @Test
    void createsNewVersionWithNextNumberAndActivatesIt() {
        AiPromptProfilePo profile = profile(11L, 7L, null);
        AiPromptVersionPo inserted = version(22L, 11L, 2, "new prompt");
        when(profileMapper.selectOwnedByIdForUpdate(11L, 7L)).thenReturn(profile);
        when(versionMapper.selectOne(any())).thenReturn(null, inserted);
        when(versionMapper.selectMaxVersionNo(11L)).thenReturn(1);
        when(versionMapper.insert(any(AiPromptVersionPo.class))).thenAnswer(invocation -> {
            AiPromptVersionPo value = invocation.getArgument(0);
            value.setId(22L);
            return 1;
        });
        when(profileMapper.updateActiveVersion(11L, 7L, 22L)).thenReturn(1);

        PromptVersionChange change = service.createVersion(11L, "new prompt");

        assertTrue(change.created());
        assertEquals(2, change.version().versionNo());
        assertEquals(22L, change.version().id());
        ArgumentCaptor<AiPromptVersionPo> insertedCaptor =
                ArgumentCaptor.forClass(AiPromptVersionPo.class);
        verify(versionMapper).insert(insertedCaptor.capture());
        assertEquals(64, insertedCaptor.getValue().getContentHash().length());
        assertEquals("new prompt", insertedCaptor.getValue().getContent());
    }

    @Test
    void reusesSameContentVersionAndMakesItActive() {
        AiPromptProfilePo profile = profile(11L, 7L, 20L);
        AiPromptVersionPo existing = version(21L, 11L, 1, "same prompt");
        when(profileMapper.selectOwnedByIdForUpdate(11L, 7L)).thenReturn(profile);
        when(versionMapper.selectOne(any())).thenReturn(existing);
        when(profileMapper.updateActiveVersion(11L, 7L, 21L)).thenReturn(1);

        PromptVersionChange change = service.createVersion(11L, "same prompt");

        assertFalse(change.created());
        assertEquals(21L, change.version().id());
        verify(versionMapper, never()).insert(any(AiPromptVersionPo.class));
        verify(versionMapper, never()).selectMaxVersionNo(11L);
    }

    @Test
    void rejectsCrossOwnerProfileWithoutReadingVersions() {
        when(profileMapper.selectOwnedByIdForUpdate(99L, 7L)).thenReturn(null);

        PromptNotFoundException exception = assertThrows(
                PromptNotFoundException.class,
                () -> service.createVersion(99L, "prompt"));

        assertEquals("PROMPT_PROFILE_NOT_FOUND", exception.getCode());
        verify(versionMapper, never()).selectOne(any());
    }

    @Test
    void rejectsVersionThatDoesNotBelongToOwnedProfile() {
        when(profileMapper.selectOwnedByIdForUpdate(11L, 7L))
                .thenReturn(profile(11L, 7L, 20L));
        when(versionMapper.selectOne(any())).thenReturn(null);

        PromptNotFoundException exception = assertThrows(
                PromptNotFoundException.class,
                () -> service.activateVersion(11L, 88L));

        assertEquals("PROMPT_VERSION_NOT_FOUND", exception.getCode());
        verify(profileMapper, never()).updateActiveVersion(anyLong(), anyLong(), anyLong());
    }

    @Test
    void enforcesPromptUnicodeCharacterLimitBeforePersistence() {
        String tooLong = "😀".repeat(8_001);

        PromptRequestException exception = assertThrows(
                PromptRequestException.class,
                () -> service.createVersion(11L, tooLong));

        assertEquals("PROMPT_CONTENT_TOO_LONG", exception.getCode());
        verify(profileMapper, never()).selectOwnedByIdForUpdate(11L, 7L);
    }

    @Test
    void mapsConcurrentProfileNameConflictToStableBusinessError() {
        when(profileMapper.insert(any(AiPromptProfilePo.class)))
                .thenThrow(new DuplicateKeyException("duplicate profile name"));

        PromptConflictException exception = assertThrows(
                PromptConflictException.class,
                () -> service.createProfile(" Default ", "JOB_USER_RELEVANCE"));

        assertEquals("PROMPT_PROFILE_NAME_CONFLICT", exception.getCode());
    }

    @Test
    void rejectsUnsupportedDefinitionBeforePersistence() {
        PromptRequestException exception = assertThrows(
                PromptRequestException.class,
                () -> service.createProfile("Default", "USER_DEFINED"));

        assertEquals("UNSUPPORTED_ANALYSIS_DEFINITION", exception.getCode());
        verify(profileMapper, never()).insert(any(AiPromptProfilePo.class));
    }

    @Test
    void rejectsInvalidStatusBeforeLockingProfile() {
        PromptRequestException exception = assertThrows(
                PromptRequestException.class,
                () -> service.updateStatus(11L, "DELETED"));

        assertEquals("INVALID_PROMPT_PROFILE_STATUS", exception.getCode());
        verify(profileMapper, never()).selectOwnedByIdForUpdate(anyLong(), anyLong());
    }

    @Test
    void updatesOnlyStatusForOwnedProfile() {
        AiPromptProfilePo current = profile(11L, 7L, 20L);
        AiPromptProfilePo updated = profile(11L, 7L, 20L);
        updated.setStatus("DISABLED");
        updated.setUpdatedAt(LocalDateTime.of(2026, 8, 3, 10, 1));
        when(profileMapper.selectOwnedByIdForUpdate(11L, 7L)).thenReturn(current);
        when(profileMapper.updateStatus(11L, 7L, "DISABLED")).thenReturn(1);
        when(profileMapper.selectOne(any())).thenReturn(updated);

        assertEquals("DISABLED", service.updateStatus(11L, "DISABLED").status().name());

        verify(profileMapper).updateStatus(11L, 7L, "DISABLED");
        verify(profileMapper, never()).updateById(any(AiPromptProfilePo.class));
    }

    @Test
    void leavesAlreadyMatchingStatusUnchanged() {
        AiPromptProfilePo current = profile(11L, 7L, 20L);
        when(profileMapper.selectOwnedByIdForUpdate(11L, 7L)).thenReturn(current);

        assertEquals("ACTIVE", service.updateStatus(11L, "ACTIVE").status().name());

        verify(profileMapper, never()).updateStatus(anyLong(), anyLong(), anyString());
    }

    private AiPromptProfilePo profile(long id, long userId, Long activeVersionId) {
        AiPromptProfilePo profile = new AiPromptProfilePo();
        profile.setId(id);
        profile.setUserId(userId);
        profile.setName("Default");
        profile.setAnalysisDefinitionKey("JOB_USER_RELEVANCE");
        profile.setActiveVersionId(activeVersionId);
        profile.setStatus("ACTIVE");
        profile.setCreatedAt(LocalDateTime.of(2026, 8, 3, 10, 0));
        profile.setUpdatedAt(LocalDateTime.of(2026, 8, 3, 10, 0));
        return profile;
    }

    private AiPromptVersionPo version(
            long id, long profileId, int versionNo, String content) {
        AiPromptVersionPo version = new AiPromptVersionPo();
        version.setId(id);
        version.setPromptProfileId(profileId);
        version.setVersionNo(versionNo);
        version.setContent(content);
        version.setContentHash(new PromptContentHasher().hash(content));
        version.setCreatedAt(LocalDateTime.of(2026, 8, 3, 10, versionNo));
        return version;
    }
}
