package com.informationplatform.hub.identity.infrastructure.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.informationplatform.hub.identity.domain.UsernameNormalizer;
import com.informationplatform.hub.identity.infrastructure.persistence.mapper.UserAccountMapper;
import com.informationplatform.hub.identity.infrastructure.persistence.po.UserAccountPo;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** 从已实施的 user_account 表加载登录身份。 */
@Service
public class IdentityUserDetailsService implements UserDetailsService {

    /** 用户账号持久化 Mapper。 */
    private final UserAccountMapper userAccountMapper;

    public IdentityUserDetailsService(UserAccountMapper userAccountMapper) {
        this.userAccountMapper = userAccountMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalizedUsername = UsernameNormalizer.normalize(username);
        UserAccountPo account = userAccountMapper.selectOne(Wrappers.<UserAccountPo>lambdaQuery()
                .eq(UserAccountPo::getUsername, normalizedUsername));
        if (account == null) {
            // 对外认证失败统一处理，不泄露账号是否存在。
            throw new UsernameNotFoundException("Invalid username or password");
        }
        return new IdentityPrincipal(
                account.getId(),
                account.getUsername(),
                account.getPasswordHash(),
                account.getDisplayName(),
                account.getTimezone(),
                "ACTIVE".equals(account.getStatus()));
    }
}
