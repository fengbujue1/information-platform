package com.informationplatform.hub.ingestion.domain;

public record ArchiveContent(
        /** 与具体业务类型无关的通用信息字段。 */
        InformationFields information,
        /** JOB 类型对应的职位扩展字段。 */
        JobFields job) {}
