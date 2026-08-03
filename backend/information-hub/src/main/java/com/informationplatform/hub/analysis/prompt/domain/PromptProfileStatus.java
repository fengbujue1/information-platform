package com.informationplatform.hub.analysis.prompt.domain;

/** Prompt Profile 可用状态。 */
public enum PromptProfileStatus {
    /** Profile 可被正常配置和后续任务使用。 */
    ACTIVE,
    /** Profile 已停用但历史 Version 保留。 */
    DISABLED
}
