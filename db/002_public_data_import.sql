-- Phase 1 public data import staging schema.
-- Target: MySQL 8.x, utf8mb4.

SET NAMES utf8mb4;
SET time_zone = '+09:00';

CREATE TABLE IF NOT EXISTS public_data_import_runs (
    import_run_id BIGINT NOT NULL AUTO_INCREMENT,
    status ENUM('RUNNING', 'SUCCEEDED', 'FAILED') NOT NULL,
    target_months INT NOT NULL,
    target_regions VARCHAR(255) NOT NULL,
    dry_run BOOLEAN NOT NULL DEFAULT TRUE,
    fetched_count INT NOT NULL DEFAULT 0,
    staged_count INT NOT NULL DEFAULT 0,
    geocoded_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    started_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    finished_at TIMESTAMP(6) NULL,
    failure_reason VARCHAR(1000) NULL,
    PRIMARY KEY (import_run_id),
    KEY idx_public_data_import_runs_status_started (status, started_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS public_data_import_errors (
    import_error_id BIGINT NOT NULL AUTO_INCREMENT,
    import_run_id BIGINT NULL,
    lawd_cd VARCHAR(5) NULL,
    deal_ymd CHAR(6) NULL,
    api_type ENUM('SALE', 'RENT') NULL,
    error_code VARCHAR(100) NOT NULL,
    message VARCHAR(1000) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (import_error_id),
    KEY idx_public_data_import_errors_run (import_run_id),
    KEY idx_public_data_import_errors_scope (lawd_cd, deal_ymd, api_type),
    CONSTRAINT fk_public_data_import_errors_run
        FOREIGN KEY (import_run_id) REFERENCES public_data_import_runs (import_run_id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS public_data_geocoding_cache (
    address_key VARCHAR(500) NOT NULL,
    full_address VARCHAR(700) NOT NULL,
    provider VARCHAR(50) NOT NULL DEFAULT 'KAKAO_LOCAL',
    status ENUM('PENDING', 'SUCCESS', 'ZERO_RESULT', 'ERROR') NOT NULL DEFAULT 'PENDING',
    latitude DECIMAL(10, 7) NULL,
    longitude DECIMAL(10, 7) NULL,
    failure_reason VARCHAR(500) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (address_key),
    KEY idx_public_data_geocoding_status (status, updated_at DESC),
    KEY idx_public_data_geocoding_bounds (latitude, longitude),
    CONSTRAINT chk_public_data_geocoding_latitude CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_public_data_geocoding_longitude CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180),
    CONSTRAINT chk_public_data_geocoding_success_coordinates CHECK (
        status <> 'SUCCESS'
        OR (latitude IS NOT NULL AND longitude IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS public_data_raw_apartment_transactions (
    raw_transaction_id BIGINT NOT NULL AUTO_INCREMENT,
    import_run_id BIGINT NULL,
    source_system VARCHAR(50) NOT NULL DEFAULT 'PUBLIC_DATA_PORTAL',
    source_key VARCHAR(500) NOT NULL,
    transaction_type ENUM('SALE', 'JEONSE', 'MONTHLY_RENT') NOT NULL,
    lawd_cd VARCHAR(5) NOT NULL,
    sido VARCHAR(100) NOT NULL,
    sigungu VARCHAR(100) NOT NULL,
    legal_dong VARCHAR(100) NOT NULL,
    jibun VARCHAR(100) NOT NULL,
    address_key VARCHAR(500) NOT NULL,
    full_address VARCHAR(700) NOT NULL,
    apartment_name VARCHAR(255) NOT NULL,
    exclusive_area_m2 DECIMAL(10, 4) NULL,
    floor INT NULL,
    built_year SMALLINT NULL,
    deal_date DATE NOT NULL,
    deal_amount_krw BIGINT NULL,
    deposit_amount_krw BIGINT NULL,
    monthly_rent_krw BIGINT NULL,
    geocoding_status ENUM('PENDING', 'SUCCESS', 'ZERO_RESULT', 'ERROR') NOT NULL DEFAULT 'PENDING',
    canonical_status ENUM('NOT_READY', 'ELIGIBLE', 'APPLIED', 'SKIPPED', 'FAILED') NOT NULL DEFAULT 'NOT_READY',
    raw_payload JSON NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (raw_transaction_id),
    UNIQUE KEY uk_public_data_raw_source_key (source_key),
    KEY idx_public_data_raw_run (import_run_id),
    KEY idx_public_data_raw_region_date (sido, sigungu, legal_dong, deal_date DESC),
    KEY idx_public_data_raw_address (address_key),
    KEY idx_public_data_raw_geocoding (geocoding_status),
    KEY idx_public_data_raw_canonical (canonical_status),
    CONSTRAINT fk_public_data_raw_import_run
        FOREIGN KEY (import_run_id) REFERENCES public_data_import_runs (import_run_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_public_data_raw_geocoding
        FOREIGN KEY (address_key) REFERENCES public_data_geocoding_cache (address_key)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Canonical policy:
-- 1. Save raw apartment transactions first with a unique source_key.
-- 2. Build full_address/address_key from sido + sigungu + legal_dong + jibun.
-- 3. Cache Kakao geocoding status per address_key.
-- 4. Only rows with geocoding_status='SUCCESS' and a SUCCESS geocoding cache row are eligible for properties/property_transactions upsert.
-- 5. Canonical apartment identity is sido + sigungu + legal_dong + jibun/full_address + apartment_name.
-- 6. property_transactions source_system + source_transaction_id prevents duplicate canonical transactions.
