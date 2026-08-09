-- 在任何持久结构变更前拒绝无法安全归类的 V3 非 JOB disposition 数据。
CREATE TEMPORARY TABLE tmp_v4_interaction_validation (
    invalid_count BIGINT UNSIGNED NOT NULL,
    CONSTRAINT ck_tmp_v4_interaction_validation CHECK (invalid_count = 0)
);

INSERT INTO tmp_v4_interaction_validation (invalid_count)
SELECT COUNT(*)
FROM user_information_interaction interaction_state
JOIN information_item information
    ON information.id = interaction_state.information_id
WHERE information.information_type <> 'JOB'
  AND (
      interaction_state.job_disposition <> 'NONE'
      OR interaction_state.disposition_updated_at IS NOT NULL
  );

DROP TEMPORARY TABLE tmp_v4_interaction_validation;

ALTER TABLE user_recommendation_profile
    ADD COLUMN information_type VARCHAR(32) NULL AFTER user_id;

-- V3 Profile 只服务 JOB，统一回填为 JOB 后再收紧 NOT NULL。
UPDATE user_recommendation_profile
SET information_type = 'JOB'
WHERE information_type IS NULL;

ALTER TABLE user_recommendation_profile
    MODIFY COLUMN information_type VARCHAR(32) NOT NULL;

CREATE TABLE job_recommendation_profile (
    profile_id BIGINT UNSIGNED NOT NULL,
    target_roles JSON NULL,
    preferred_skills JSON NULL,
    preferred_cities JSON NULL,
    preferred_remote_types JSON NULL,
    salary_min_monthly_yuan INT UNSIGNED NULL,
    excluded_keywords JSON NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (profile_id),
    CONSTRAINT fk_job_recommendation_profile_core
        FOREIGN KEY (profile_id) REFERENCES user_recommendation_profile (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO job_recommendation_profile (
    profile_id,
    target_roles,
    preferred_skills,
    preferred_cities,
    preferred_remote_types,
    salary_min_monthly_yuan,
    excluded_keywords,
    created_at,
    updated_at
)
SELECT
    id,
    target_roles,
    preferred_skills,
    preferred_cities,
    preferred_remote_types,
    salary_min_monthly_yuan,
    excluded_keywords,
    created_at,
    updated_at
FROM user_recommendation_profile
WHERE information_type = 'JOB';

CREATE TEMPORARY TABLE tmp_v4_profile_copy_validation (
    expected_count BIGINT UNSIGNED NOT NULL,
    actual_count BIGINT UNSIGNED NOT NULL,
    CONSTRAINT ck_tmp_v4_profile_copy_validation CHECK (expected_count = actual_count)
);

INSERT INTO tmp_v4_profile_copy_validation (expected_count, actual_count)
SELECT
    (SELECT COUNT(*) FROM user_recommendation_profile WHERE information_type = 'JOB'),
    (SELECT COUNT(*) FROM job_recommendation_profile);

DROP TEMPORARY TABLE tmp_v4_profile_copy_validation;

ALTER TABLE user_recommendation_profile
    DROP INDEX uk_user_recommendation_profile_user,
    ADD UNIQUE KEY uk_user_recommendation_profile_identity (user_id, information_type),
    DROP COLUMN target_roles,
    DROP COLUMN preferred_skills,
    DROP COLUMN preferred_cities,
    DROP COLUMN preferred_remote_types,
    DROP COLUMN salary_min_monthly_yuan,
    DROP COLUMN excluded_keywords;

CREATE TABLE user_job_disposition (
    interaction_id BIGINT UNSIGNED NOT NULL,
    job_disposition VARCHAR(32) NOT NULL DEFAULT 'NONE',
    disposition_updated_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (interaction_id),
    CONSTRAINT fk_user_job_disposition_interaction
        FOREIGN KEY (interaction_id) REFERENCES user_information_interaction (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO user_job_disposition (
    interaction_id,
    job_disposition,
    disposition_updated_at,
    created_at,
    updated_at
)
SELECT
    interaction_state.id,
    interaction_state.job_disposition,
    interaction_state.disposition_updated_at,
    interaction_state.created_at,
    interaction_state.updated_at
FROM user_information_interaction interaction_state
JOIN information_item information
    ON information.id = interaction_state.information_id
WHERE information.information_type = 'JOB';

CREATE TEMPORARY TABLE tmp_v4_disposition_copy_validation (
    expected_count BIGINT UNSIGNED NOT NULL,
    actual_count BIGINT UNSIGNED NOT NULL,
    CONSTRAINT ck_tmp_v4_disposition_copy_validation CHECK (expected_count = actual_count)
);

INSERT INTO tmp_v4_disposition_copy_validation (expected_count, actual_count)
SELECT
    (
        SELECT COUNT(*)
        FROM user_information_interaction interaction_state
        JOIN information_item information
            ON information.id = interaction_state.information_id
        WHERE information.information_type = 'JOB'
    ),
    (SELECT COUNT(*) FROM user_job_disposition);

DROP TEMPORARY TABLE tmp_v4_disposition_copy_validation;

ALTER TABLE user_information_interaction
    DROP INDEX idx_user_information_interaction_state,
    ADD KEY idx_user_information_interaction_state
        (user_id, feedback_state, updated_at),
    DROP COLUMN job_disposition,
    DROP COLUMN disposition_updated_at;

ALTER TABLE recommendation_run
    ADD COLUMN information_type VARCHAR(32) NULL AFTER user_id;

UPDATE recommendation_run recommendation
JOIN user_recommendation_profile profile
    ON profile.id = recommendation.profile_id
SET recommendation.information_type = profile.information_type,
    recommendation.profile_snapshot_json = JSON_SET(
        recommendation.profile_snapshot_json,
        '$.informationType',
        profile.information_type
    )
WHERE recommendation.information_type IS NULL;

CREATE TEMPORARY TABLE tmp_v4_run_validation (
    invalid_count BIGINT UNSIGNED NOT NULL,
    CONSTRAINT ck_tmp_v4_run_validation CHECK (invalid_count = 0)
);

INSERT INTO tmp_v4_run_validation (invalid_count)
SELECT COUNT(*)
FROM recommendation_run
WHERE information_type IS NULL;

DROP TEMPORARY TABLE tmp_v4_run_validation;

ALTER TABLE recommendation_run
    MODIFY COLUMN information_type VARCHAR(32) NOT NULL,
    DROP INDEX idx_recommendation_run_user_status_created,
    DROP INDEX idx_recommendation_run_user_completed,
    ADD KEY idx_recommendation_run_user_type_status_created
        (user_id, information_type, status, created_at),
    ADD KEY idx_recommendation_run_user_type_completed
        (user_id, information_type, completed_at, id);
