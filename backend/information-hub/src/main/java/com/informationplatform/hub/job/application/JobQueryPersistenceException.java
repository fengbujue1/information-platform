package com.informationplatform.hub.job.application;

/** 隐藏职位查询中的底层数据库异常细节。 */
public class JobQueryPersistenceException extends RuntimeException {

    public JobQueryPersistenceException(Throwable cause) {
        super("Job query persistence failed", cause);
    }
}
