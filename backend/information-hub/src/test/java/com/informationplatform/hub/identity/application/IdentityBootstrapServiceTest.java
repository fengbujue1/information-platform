package com.informationplatform.hub.identity.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.informationplatform.hub.identity.infrastructure.bootstrap.IdentityBootstrapProperties;
import com.informationplatform.hub.identity.infrastructure.persistence.mapper.UserAccountMapper;
import com.informationplatform.hub.identity.infrastructure.persistence.po.UserAccountPo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/** 验证一次性 Bootstrap 不保存明文且仅在空表执行。 */
@ExtendWith(MockitoExtension.class)
class IdentityBootstrapServiceTest {

    @Mock
    private UserAccountMapper userAccountMapper;

    private final PasswordEncoder passwordEncoder =
            PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @Test
    void createsNormalizedActiveAccountWithPasswordHash() {
        when(userAccountMapper.selectCount(any())).thenReturn(0L);
        when(userAccountMapper.insert(any(UserAccountPo.class))).thenReturn(1);
        IdentityBootstrapService service =
                new IdentityBootstrapService(userAccountMapper, passwordEncoder);

        boolean created = service.bootstrap(properties(" Admin ", "secret"));

        assertTrue(created);
        ArgumentCaptor<UserAccountPo> captor = ArgumentCaptor.forClass(UserAccountPo.class);
        verify(userAccountMapper).insert(captor.capture());
        UserAccountPo inserted = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("admin", inserted.getUsername());
        org.junit.jupiter.api.Assertions.assertEquals("ACTIVE", inserted.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("Asia/Shanghai", inserted.getTimezone());
        org.junit.jupiter.api.Assertions.assertNotEquals("secret", inserted.getPasswordHash());
        assertTrue(passwordEncoder.matches("secret", inserted.getPasswordHash()));
    }

    @Test
    void skipsWhenConfigurationMissingOrAnyAccountExists() {
        IdentityBootstrapService service =
                new IdentityBootstrapService(userAccountMapper, passwordEncoder);

        assertFalse(service.bootstrap(properties("", "")));
        verify(userAccountMapper, never()).insert(any(UserAccountPo.class));

        when(userAccountMapper.selectCount(any())).thenReturn(1L);
        assertFalse(service.bootstrap(properties("admin", "secret")));
        verify(userAccountMapper, never()).insert(any(UserAccountPo.class));
    }

    @Test
    void treatsConcurrentUniqueConstraintConflictAsAlreadyBootstrapped() {
        when(userAccountMapper.selectCount(any())).thenReturn(0L);
        when(userAccountMapper.insert(any(UserAccountPo.class)))
                .thenThrow(new DuplicateKeyException("concurrent bootstrap"));
        IdentityBootstrapService service =
                new IdentityBootstrapService(userAccountMapper, passwordEncoder);

        assertFalse(service.bootstrap(properties("admin", "secret")));
    }

    private IdentityBootstrapProperties properties(String username, String password) {
        IdentityBootstrapProperties properties = new IdentityBootstrapProperties();
        properties.setUsername(username);
        properties.setPassword(password);
        properties.setDisplayName("Admin");
        properties.setTimezone("Asia/Shanghai");
        return properties;
    }
}
