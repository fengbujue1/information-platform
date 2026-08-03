package com.informationplatform.hub.identity.application;

import com.informationplatform.hub.identity.domain.AuthenticatedUser;

/** 为后续业务 Service 提供不可由客户端伪造的 Owner 身份。 */
public interface CurrentUserProvider {

    /** 返回当前已认证 Session 对应的用户。 */
    AuthenticatedUser requireCurrentUser();
}
