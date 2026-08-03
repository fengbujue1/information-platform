ALTER TABLE information_item
    ADD KEY idx_information_item_type_first_seen_id
        (information_type, first_seen_time, id);

CREATE TABLE user_account (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NULL,
    timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Shanghai',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_account_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE ai_prompt_profile (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(255) NOT NULL,
    analysis_definition_key VARCHAR(128) NOT NULL,
    active_version_id BIGINT UNSIGNED NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_prompt_profile_user_name (user_id, name),
    KEY idx_ai_prompt_profile_user_status (user_id, status),
    CONSTRAINT fk_ai_prompt_profile_user
        FOREIGN KEY (user_id) REFERENCES user_account (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE ai_prompt_version (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    prompt_profile_id BIGINT UNSIGNED NOT NULL,
    version_no INT UNSIGNED NOT NULL,
    content LONGTEXT NOT NULL,
    content_hash CHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_prompt_version_no (prompt_profile_id, version_no),
    UNIQUE KEY uk_ai_prompt_version_content (prompt_profile_id, content_hash),
    KEY idx_ai_prompt_version_profile_created (prompt_profile_id, created_at),
    CONSTRAINT fk_ai_prompt_version_profile
        FOREIGN KEY (prompt_profile_id) REFERENCES ai_prompt_profile (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE ai_prompt_profile
    ADD CONSTRAINT fk_ai_prompt_profile_active_version
        FOREIGN KEY (active_version_id) REFERENCES ai_prompt_version (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT;

CREATE TABLE information_analysis (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    information_id BIGINT UNSIGNED NOT NULL,
    snapshot_id BIGINT UNSIGNED NOT NULL,
    information_type VARCHAR(32) NOT NULL,
    analysis_definition_key VARCHAR(128) NOT NULL,
    analysis_definition_version INT UNSIGNED NOT NULL,
    analysis_purpose VARCHAR(32) NOT NULL,
    prompt_profile_id BIGINT UNSIGNED NOT NULL,
    prompt_version_id BIGINT UNSIGNED NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    result_json JSON NULL,
    relevance_score TINYINT UNSIGNED NULL,
    summary VARCHAR(2000) NULL,
    estimated_input_tokens BIGINT UNSIGNED NULL,
    estimated_output_tokens BIGINT UNSIGNED NULL,
    estimated_total_tokens BIGINT UNSIGNED NULL,
    estimate_method VARCHAR(64) NULL,
    failure_code VARCHAR(128) NULL,
    failure_message VARCHAR(2000) NULL,
    started_at DATETIME(3) NULL,
    completed_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_information_analysis_identity (
        user_id,
        snapshot_id,
        prompt_version_id,
        analysis_definition_key,
        analysis_definition_version
    ),
    KEY idx_information_analysis_user_created (user_id, created_at),
    KEY idx_information_analysis_user_status_created (user_id, status, created_at),
    KEY idx_information_analysis_information_user (information_id, user_id),
    KEY idx_information_analysis_snapshot_user (snapshot_id, user_id),
    KEY idx_information_analysis_profile_created (prompt_profile_id, created_at),
    CONSTRAINT fk_information_analysis_user
        FOREIGN KEY (user_id) REFERENCES user_account (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_information_analysis_information
        FOREIGN KEY (information_id) REFERENCES information_item (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_information_analysis_snapshot
        FOREIGN KEY (snapshot_id) REFERENCES information_snapshot (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_information_analysis_profile
        FOREIGN KEY (prompt_profile_id) REFERENCES ai_prompt_profile (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_information_analysis_prompt_version
        FOREIGN KEY (prompt_version_id) REFERENCES ai_prompt_version (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE ai_analysis_schedule (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(255) NOT NULL,
    prompt_profile_id BIGINT UNSIGNED NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 0,
    local_time TIME NOT NULL DEFAULT '02:00:00',
    timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Shanghai',
    window_days INT UNSIGNED NOT NULL DEFAULT 3,
    max_candidates INT UNSIGNED NOT NULL DEFAULT 20,
    max_estimated_tokens BIGINT UNSIGNED NOT NULL DEFAULT 75000,
    next_run_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_analysis_schedule_user_name (user_id, name),
    KEY idx_ai_analysis_schedule_due (enabled, next_run_at, id),
    KEY idx_ai_analysis_schedule_user_enabled (user_id, enabled),
    KEY idx_ai_analysis_schedule_profile (prompt_profile_id),
    CONSTRAINT fk_ai_analysis_schedule_user
        FOREIGN KEY (user_id) REFERENCES user_account (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ai_analysis_schedule_profile
        FOREIGN KEY (prompt_profile_id) REFERENCES ai_prompt_profile (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE ai_analysis_batch (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    trigger_type VARCHAR(32) NOT NULL,
    manual_request_id CHAR(36) NULL,
    schedule_id BIGINT UNSIGNED NULL,
    scheduled_for DATETIME(3) NULL,
    information_type VARCHAR(32) NOT NULL,
    analysis_definition_key VARCHAR(128) NOT NULL,
    analysis_definition_version INT UNSIGNED NOT NULL,
    prompt_profile_id BIGINT UNSIGNED NOT NULL,
    prompt_version_id BIGINT UNSIGNED NOT NULL,
    window_basis VARCHAR(32) NOT NULL DEFAULT 'FIRST_INGESTED',
    requested_window_days INT UNSIGNED NOT NULL,
    window_start DATETIME(3) NOT NULL,
    window_end DATETIME(3) NOT NULL,
    requested_max_candidates INT UNSIGNED NOT NULL,
    requested_token_budget BIGINT UNSIGNED NOT NULL,
    total_in_window INT UNSIGNED NOT NULL DEFAULT 0,
    eligible_count INT UNSIGNED NOT NULL DEFAULT 0,
    already_analyzed_count INT UNSIGNED NOT NULL DEFAULT 0,
    selected_count INT UNSIGNED NOT NULL DEFAULT 0,
    deferred_by_item_limit_count INT UNSIGNED NOT NULL DEFAULT 0,
    deferred_by_token_budget_count INT UNSIGNED NOT NULL DEFAULT 0,
    estimated_input_tokens BIGINT UNSIGNED NULL,
    estimated_output_tokens BIGINT UNSIGNED NULL,
    estimated_total_tokens BIGINT UNSIGNED NULL,
    estimate_method VARCHAR(64) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    skip_reason VARCHAR(64) NULL,
    started_at DATETIME(3) NULL,
    completed_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_analysis_batch_manual (user_id, manual_request_id),
    UNIQUE KEY uk_ai_analysis_batch_schedule (schedule_id, scheduled_for),
    KEY idx_ai_analysis_batch_user_created (user_id, created_at),
    KEY idx_ai_analysis_batch_user_status_created (user_id, status, created_at),
    KEY idx_ai_analysis_batch_schedule_status (schedule_id, status),
    KEY idx_ai_analysis_batch_profile_created (prompt_profile_id, created_at),
    CONSTRAINT ck_ai_analysis_batch_trigger_fields CHECK (
        (
            trigger_type = 'MANUAL'
            AND manual_request_id IS NOT NULL
            AND schedule_id IS NULL
            AND scheduled_for IS NULL
        )
        OR
        (
            trigger_type = 'SCHEDULED'
            AND manual_request_id IS NULL
            AND schedule_id IS NOT NULL
            AND scheduled_for IS NOT NULL
        )
    ),
    CONSTRAINT fk_ai_analysis_batch_user
        FOREIGN KEY (user_id) REFERENCES user_account (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ai_analysis_batch_schedule
        FOREIGN KEY (schedule_id) REFERENCES ai_analysis_schedule (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ai_analysis_batch_profile
        FOREIGN KEY (prompt_profile_id) REFERENCES ai_prompt_profile (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ai_analysis_batch_prompt_version
        FOREIGN KEY (prompt_version_id) REFERENCES ai_prompt_version (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE ai_analysis_batch_item (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    batch_id BIGINT UNSIGNED NOT NULL,
    information_id BIGINT UNSIGNED NOT NULL,
    snapshot_id BIGINT UNSIGNED NOT NULL,
    analysis_id BIGINT UNSIGNED NULL,
    selection_order INT UNSIGNED NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'SELECTED',
    decision_reason VARCHAR(64) NULL,
    estimated_input_tokens BIGINT UNSIGNED NULL,
    estimated_output_tokens BIGINT UNSIGNED NULL,
    estimated_total_tokens BIGINT UNSIGNED NULL,
    started_at DATETIME(3) NULL,
    completed_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_analysis_batch_item_snapshot (batch_id, snapshot_id),
    UNIQUE KEY uk_ai_analysis_batch_item_order (batch_id, selection_order),
    KEY idx_ai_analysis_batch_item_status (batch_id, status),
    KEY idx_ai_analysis_batch_item_analysis (analysis_id),
    KEY idx_ai_analysis_batch_item_snapshot (snapshot_id),
    CONSTRAINT fk_ai_analysis_batch_item_batch
        FOREIGN KEY (batch_id) REFERENCES ai_analysis_batch (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ai_analysis_batch_item_information
        FOREIGN KEY (information_id) REFERENCES information_item (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ai_analysis_batch_item_snapshot
        FOREIGN KEY (snapshot_id) REFERENCES information_snapshot (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ai_analysis_batch_item_analysis
        FOREIGN KEY (analysis_id) REFERENCES information_analysis (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE ai_model_invocation (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    analysis_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    batch_item_id BIGINT UNSIGNED NULL,
    provider VARCHAR(64) NOT NULL,
    model_name VARCHAR(255) NOT NULL,
    provider_request_id VARCHAR(255) NULL,
    attempt_no INT UNSIGNED NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'RUNNING',
    finish_reason VARCHAR(128) NULL,
    input_tokens BIGINT UNSIGNED NULL,
    output_tokens BIGINT UNSIGNED NULL,
    total_tokens BIGINT UNSIGNED NULL,
    cached_input_tokens BIGINT UNSIGNED NULL,
    reasoning_tokens BIGINT UNSIGNED NULL,
    usage_status VARCHAR(32) NOT NULL DEFAULT 'UNAVAILABLE',
    latency_ms BIGINT UNSIGNED NULL,
    error_code VARCHAR(128) NULL,
    error_message VARCHAR(2000) NULL,
    started_at DATETIME(3) NOT NULL,
    completed_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_model_invocation_attempt (analysis_id, attempt_no),
    KEY idx_ai_model_invocation_user_created (user_id, created_at),
    KEY idx_ai_model_invocation_analysis_created (analysis_id, created_at),
    KEY idx_ai_model_invocation_batch_item_created (batch_item_id, created_at),
    KEY idx_ai_model_invocation_provider_request (provider, provider_request_id),
    CONSTRAINT fk_ai_model_invocation_analysis
        FOREIGN KEY (analysis_id) REFERENCES information_analysis (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ai_model_invocation_user
        FOREIGN KEY (user_id) REFERENCES user_account (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ai_model_invocation_batch_item
        FOREIGN KEY (batch_item_id) REFERENCES ai_analysis_batch_item (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
