package com.informationplatform.hub.identity.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.informationplatform.hub.identity.domain.UsernameNormalizer;
import com.informationplatform.hub.identity.infrastructure.bootstrap.IdentityBootstrapProperties;
import com.informationplatform.hub.identity.infrastructure.persistence.mapper.UserAccountMapper;
import com.informationplatform.hub.identity.infrastructure.persistence.po.UserAccountPo;
import java.time.ZoneId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 仅在账号表为空时创建一次受控初始账号。 */
@Service
public class IdentityBootstrapService {

    /** 用户账号持久化 Mapper。 */
    private final UserAccountMapper userAccountMapper;

    /** 只保存安全摘要的密码编码器。 */
    private final PasswordEncoder passwordEncoder;

    public IdentityBootstrapService(
            UserAccountMapper userAccountMapper,
            PasswordEncoder passwordEncoder) {
        this.userAccountMapper = userAccountMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 在单个事务内检查并创建初始账号。
     *
     * <p>并发启动时以用户名唯一约束作为最终幂等保护；任何已有账号都会阻止 Bootstrap。
     */
    @Transactional
    public boolean bootstrap(IdentityBootstrapProperties properties) {
        String username = UsernameNormalizer.normalize(properties.getUsername());
        String password = properties.getPassword();
        if (username.isBlank() || password == null || password.isBlank()) {
            return false;
        }
        if (userAccountMapper.selectCount(Wrappers.<UserAccountPo>query()) > 0) {
            return false;
        }

        UserAccountPo account = new UserAccountPo();
        account.setUsername(username);
        // 明文只在内存中交给 PasswordEncoder，持久化对象只接收摘要。
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setDisplayName(normalizeNullable(properties.getDisplayName()));
        account.setTimezone(ZoneId.of(properties.getTimezone()).getId());
        account.setStatus("ACTIVE");
        try {
            return userAccountMapper.insert(account) == 1;
        } catch (DuplicateKeyException exception) {
            // 同一配置并发启动时，唯一约束冲突表示另一实例已完成 Bootstrap。
            return false;
        }
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
