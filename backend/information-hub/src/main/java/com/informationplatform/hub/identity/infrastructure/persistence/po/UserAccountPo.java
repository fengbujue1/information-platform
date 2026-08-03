package com.informationplatform.hub.identity.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 映射 Phase 3 最小用户账号表。 */
@TableName("user_account")
public class UserAccountPo {

    /** 用户账号自增主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 经过去空白和小写规范化的唯一登录名。 */
    private String username;

    /** 使用安全密码编码器生成的密码摘要，禁止保存明文。 */
    private String passwordHash;

    /** 用户在页面中展示的名称。 */
    private String displayName;

    /** 用户的 IANA 时区名称，默认 Asia/Shanghai。 */
    private String timezone;

    /** 账号状态，例如 ACTIVE 或 DISABLED。 */
    private String status;

    /** 账号创建时间，按 UTC 保存。 */
    private LocalDateTime createdAt;

    /** 账号最近更新时间，按 UTC 保存。 */
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
