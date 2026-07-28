package com.informationplatform.hub.job.application;

/** 表示请求的职位主键不存在或不属于 JOB 信息。 */
public class JobNotFoundException extends RuntimeException {

    public JobNotFoundException(long informationId) {
        super("Job does not exist: " + informationId);
    }
}
