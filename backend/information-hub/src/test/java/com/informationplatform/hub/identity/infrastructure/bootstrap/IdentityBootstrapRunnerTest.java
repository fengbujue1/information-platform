package com.informationplatform.hub.identity.infrastructure.bootstrap;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.informationplatform.hub.identity.application.IdentityBootstrapService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

/** 验证未显式配置初始凭据时 Bootstrap 不访问数据库。 */
@ExtendWith(MockitoExtension.class)
class IdentityBootstrapRunnerTest {

    @Mock
    private IdentityBootstrapService bootstrapService;

    @Mock
    private ApplicationArguments applicationArguments;

    @Test
    void skipsServiceWhenBootstrapCredentialsAreMissing() {
        IdentityBootstrapProperties properties = new IdentityBootstrapProperties();
        IdentityBootstrapRunner runner =
                new IdentityBootstrapRunner(properties, bootstrapService);

        runner.run(applicationArguments);

        verify(bootstrapService, never()).bootstrap(properties);
    }

    @Test
    void delegatesWhenBootstrapCredentialsAreExplicitlyConfigured() {
        IdentityBootstrapProperties properties = new IdentityBootstrapProperties();
        properties.setUsername("admin");
        properties.setPassword("secret");
        IdentityBootstrapRunner runner =
                new IdentityBootstrapRunner(properties, bootstrapService);

        runner.run(applicationArguments);

        verify(bootstrapService).bootstrap(properties);
    }
}
