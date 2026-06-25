-- Public data import extension for officetel and villa transaction APIs.
-- Safe to re-run on existing local/dev databases before importing non-apartment data.

SET NAMES utf8mb4;
SET time_zone = '+09:00';

DELIMITER //
DROP PROCEDURE IF EXISTS jiber_public_data_006_migrate//
CREATE PROCEDURE jiber_public_data_006_migrate()
BEGIN
    DECLARE property_type_column_count BIGINT DEFAULT 0;
    DECLARE property_type_index_count BIGINT DEFAULT 0;

    ALTER TABLE public_data_import_errors
        MODIFY api_type ENUM(
            'SALE',
            'RENT',
            'OFFICETEL_SALE',
            'OFFICETEL_RENT',
            'VILLA_SALE',
            'VILLA_RENT'
        ) NULL;

    SELECT COUNT(*)
    INTO property_type_column_count
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'public_data_raw_apartment_transactions'
      AND COLUMN_NAME = 'property_type';

    IF property_type_column_count = 0 THEN
        ALTER TABLE public_data_raw_apartment_transactions
            ADD COLUMN property_type ENUM('APARTMENT', 'OFFICETEL', 'VILLA', 'HOUSE') NOT NULL DEFAULT 'APARTMENT'
                AFTER source_key;
    END IF;

    SELECT COUNT(*)
    INTO property_type_index_count
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'public_data_raw_apartment_transactions'
      AND INDEX_NAME = 'idx_public_data_raw_property_type';

    IF property_type_index_count = 0 THEN
        ALTER TABLE public_data_raw_apartment_transactions
            ADD KEY idx_public_data_raw_property_type (property_type, deal_date DESC);
    END IF;
END//
CALL jiber_public_data_006_migrate()//
DROP PROCEDURE IF EXISTS jiber_public_data_006_migrate//
DELIMITER ;
