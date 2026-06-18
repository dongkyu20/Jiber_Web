-- Property transaction source id migration for existing local/dev databases.
-- Run after 004_auth_account_social_link.sql on databases created before
-- db/001_phase1_schema.sql included uk_transactions_source and VARCHAR(500)
-- source transaction ids.
--
-- Idempotency: this file is safe to re-run. Once source_transaction_id is
-- VARCHAR(500) and uk_transactions_source exists, it becomes a no-op.
--
-- NULL policy: rows with NULL source_system or NULL source_transaction_id are
-- treated as legacy/manual rows outside source idempotency. MySQL unique indexes
-- allow multiple NULL values, so the duplicate preflight and unique key enforce
-- only complete source identities.
--
-- The duplicate preflight intentionally fails before any DDL. The error message
-- does not include actual source_transaction_id values.

SET NAMES utf8mb4;
SET time_zone = '+09:00';

DELIMITER //
DROP PROCEDURE IF EXISTS jiber_property_tx_005_migrate//
CREATE PROCEDURE jiber_property_tx_005_migrate()
BEGIN
    DECLARE duplicate_source_count BIGINT DEFAULT 0;
    DECLARE source_id_length BIGINT DEFAULT 0;
    DECLARE source_unique_count BIGINT DEFAULT 0;

    SELECT COUNT(*)
    INTO duplicate_source_count
    FROM (
        SELECT source_system, source_transaction_id
        FROM property_transactions
        WHERE source_system IS NOT NULL
          AND source_transaction_id IS NOT NULL
        GROUP BY source_system, source_transaction_id
        HAVING COUNT(*) > 1
    ) duplicate_sources;

    IF duplicate_source_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'JIBER_PROPERTY_TX_SOURCE_DUPLICATE: clean duplicate transaction source ids before db/005_property_transaction_source_unique.sql';
    END IF;

    SELECT COALESCE(MAX(CHARACTER_MAXIMUM_LENGTH), 0)
    INTO source_id_length
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'property_transactions'
      AND COLUMN_NAME = 'source_transaction_id';

    SELECT COUNT(*)
    INTO source_unique_count
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'property_transactions'
      AND INDEX_NAME = 'uk_transactions_source';

    IF source_id_length <> 500 THEN
        IF source_unique_count > 0 THEN
            ALTER TABLE property_transactions
                DROP INDEX uk_transactions_source;
        END IF;

        ALTER TABLE property_transactions
            MODIFY source_transaction_id VARCHAR(500) NULL;
    END IF;

    SELECT COUNT(*)
    INTO source_unique_count
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'property_transactions'
      AND INDEX_NAME = 'uk_transactions_source';

    IF source_unique_count = 0 THEN
        ALTER TABLE property_transactions
            ADD UNIQUE KEY uk_transactions_source (source_system, source_transaction_id);
    END IF;
END//
CALL jiber_property_tx_005_migrate()//
DROP PROCEDURE IF EXISTS jiber_property_tx_005_migrate//
DELIMITER ;
