CREATE TABLE user_recommendation_profile (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    analysis_prompt_profile_id BIGINT UNSIGNED NOT NULL,
    window_days INT UNSIGNED NOT NULL DEFAULT 7,
    top_n INT UNSIGNED NOT NULL DEFAULT 50,
    target_roles JSON NULL,
    preferred_skills JSON NULL,
    preferred_cities JSON NULL,
    preferred_remote_types JSON NULL,
    salary_min_monthly_yuan INT UNSIGNED NULL,
    excluded_keywords JSON NULL,
    content_hash CHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_recommendation_profile_user (user_id),
    KEY idx_user_recommendation_profile_prompt (analysis_prompt_profile_id),
    CONSTRAINT fk_user_recommendation_profile_user
        FOREIGN KEY (user_id) REFERENCES user_account (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_user_recommendation_profile_prompt
        FOREIGN KEY (analysis_prompt_profile_id) REFERENCES ai_prompt_profile (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_information_interaction (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    information_id BIGINT UNSIGNED NOT NULL,
    view_count INT UNSIGNED NOT NULL DEFAULT 0,
    last_viewed_at DATETIME(3) NULL,
    feedback_state VARCHAR(32) NOT NULL DEFAULT 'NONE',
    feedback_updated_at DATETIME(3) NULL,
    job_disposition VARCHAR(32) NOT NULL DEFAULT 'NONE',
    disposition_updated_at DATETIME(3) NULL,
    last_recommendation_item_id BIGINT UNSIGNED NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_information_interaction_identity (user_id, information_id),
    KEY idx_user_information_interaction_state
        (user_id, feedback_state, job_disposition, updated_at),
    KEY idx_user_information_interaction_information (information_id),
    KEY idx_user_information_interaction_last_item (last_recommendation_item_id),
    CONSTRAINT fk_user_information_interaction_user
        FOREIGN KEY (user_id) REFERENCES user_account (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_user_information_interaction_information
        FOREIGN KEY (information_id) REFERENCES information_item (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE recommendation_run (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    trigger_type VARCHAR(32) NOT NULL,
    source_analysis_batch_id BIGINT UNSIGNED NULL,
    profile_id BIGINT UNSIGNED NOT NULL,
    profile_content_hash CHAR(64) NOT NULL,
    profile_snapshot_json JSON NOT NULL,
    prompt_profile_id BIGINT UNSIGNED NOT NULL,
    prompt_version_id BIGINT UNSIGNED NOT NULL,
    algorithm_key VARCHAR(64) NOT NULL,
    algorithm_version INT UNSIGNED NOT NULL,
    window_start DATETIME(3) NOT NULL,
    window_end DATETIME(3) NOT NULL,
    candidate_count INT UNSIGNED NOT NULL DEFAULT 0,
    eligible_count INT UNSIGNED NOT NULL DEFAULT 0,
    result_count INT UNSIGNED NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    skip_reason VARCHAR(64) NULL,
    failure_code VARCHAR(64) NULL,
    failure_message VARCHAR(1000) NULL,
    started_at DATETIME(3) NULL,
    completed_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_recommendation_run_source_batch (source_analysis_batch_id),
    KEY idx_recommendation_run_user_status_created (user_id, status, created_at),
    KEY idx_recommendation_run_user_completed (user_id, completed_at, id),
    KEY idx_recommendation_run_profile (profile_id),
    KEY idx_recommendation_run_prompt_profile (prompt_profile_id),
    KEY idx_recommendation_run_prompt_version (prompt_version_id),
    CONSTRAINT fk_recommendation_run_user
        FOREIGN KEY (user_id) REFERENCES user_account (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_recommendation_run_source_batch
        FOREIGN KEY (source_analysis_batch_id) REFERENCES ai_analysis_batch (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_recommendation_run_profile
        FOREIGN KEY (profile_id) REFERENCES user_recommendation_profile (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_recommendation_run_prompt_profile
        FOREIGN KEY (prompt_profile_id) REFERENCES ai_prompt_profile (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_recommendation_run_prompt_version
        FOREIGN KEY (prompt_version_id) REFERENCES ai_prompt_version (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE recommendation_item (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    run_id BIGINT UNSIGNED NOT NULL,
    information_id BIGINT UNSIGNED NOT NULL,
    snapshot_id BIGINT UNSIGNED NOT NULL,
    analysis_id BIGINT UNSIGNED NOT NULL,
    rank_no INT UNSIGNED NOT NULL,
    final_score DECIMAL(6,3) NOT NULL,
    ai_relevance_score DECIMAL(6,3) NOT NULL,
    profile_match_score DECIMAL(6,3) NOT NULL,
    freshness_score DECIMAL(6,3) NOT NULL,
    score_breakdown_json JSON NOT NULL,
    reasons_json JSON NOT NULL,
    duplicate_group_key CHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_recommendation_item_information (run_id, information_id),
    UNIQUE KEY uk_recommendation_item_rank (run_id, rank_no),
    KEY idx_recommendation_item_information (information_id),
    KEY idx_recommendation_item_snapshot (snapshot_id),
    KEY idx_recommendation_item_analysis (analysis_id),
    CONSTRAINT fk_recommendation_item_run
        FOREIGN KEY (run_id) REFERENCES recommendation_run (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_recommendation_item_information
        FOREIGN KEY (information_id) REFERENCES information_item (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_recommendation_item_snapshot
        FOREIGN KEY (snapshot_id) REFERENCES information_snapshot (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_recommendation_item_analysis
        FOREIGN KEY (analysis_id) REFERENCES information_analysis (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Interaction 先以 NULL 建立，Item 表创建后再补 RESTRICT FK，避免循环建表顺序问题。
ALTER TABLE user_information_interaction
    ADD CONSTRAINT fk_user_information_interaction_last_item
        FOREIGN KEY (last_recommendation_item_id) REFERENCES recommendation_item (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT;
