package com.informationplatform.hub.identity.infrastructure.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 一次性初始账号的服务端环境变量映射。 */
@Component
@ConfigurationProperties(prefix = "information-hub.identity.bootstrap")
public class IdentityBootstrapProperties {

    /** 初始登录名；由管理员确定，示例 admin；为空时不执行 Bootstrap。 */
    private String username = "";

    /** 初始明文密码；由管理员安全生成，只在启动时编码，禁止日志输出。 */
    private String password = "";

    /** 初始页面显示名；由管理员填写，示例“平台管理员”，允许为空。 */
    private String displayName = "";

    /** 初始账号 IANA 时区；从 IANA tz database 选择，示例 Asia/Shanghai。 */
    private String timezone = "Asia/Shanghai";

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
}
