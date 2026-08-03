package com.informationplatform.hub.identity.infrastructure.bootstrap;

import com.informationplatform.hub.identity.application.IdentityBootstrapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** 应用启动后触发一次受控账号 Bootstrap。 */
@Component
public class IdentityBootstrapRunner implements ApplicationRunner {

    /** Bootstrap 日志不得包含密码、摘要或其他秘密。 */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(IdentityBootstrapRunner.class);

    /** Bootstrap 服务端配置。 */
    private final IdentityBootstrapProperties properties;

    /** 原子创建初始账号的应用服务。 */
    private final IdentityBootstrapService bootstrapService;

    public IdentityBootstrapRunner(
            IdentityBootstrapProperties properties,
            IdentityBootstrapService bootstrapService) {
        this.properties = properties;
        this.bootstrapService = bootstrapService;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        // 缺少显式用户名或密码时不进入事务，也不触发任何数据库连接。
        if (!isBootstrapConfigured()) {
            return;
        }
        if (bootstrapService.bootstrap(properties)) {
            LOGGER.info("Identity bootstrap account created; remove the bootstrap password configuration");
        }
    }

    /** 只有部署环境同时提供非空用户名和密码时才允许访问账号表。 */
    private boolean isBootstrapConfigured() {
        return properties.getUsername() != null
                && !properties.getUsername().isBlank()
                && properties.getPassword() != null
                && !properties.getPassword().isBlank();
    }
}
