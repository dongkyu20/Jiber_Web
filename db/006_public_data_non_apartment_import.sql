-- Public data import extension for officetel and villa transaction APIs.
-- Safe to run once on existing local/dev databases before importing non-apartment data.

SET NAMES utf8mb4;
SET time_zone = '+09:00';

ALTER TABLE public_data_import_errors
    MODIFY api_type ENUM(
        'SALE',
        'RENT',
        'OFFICETEL_SALE',
        'OFFICETEL_RENT',
        'VILLA_SALE',
        'VILLA_RENT'
    ) NULL;

ALTER TABLE public_data_raw_apartment_transactions
    ADD COLUMN property_type ENUM('APARTMENT', 'OFFICETEL', 'VILLA', 'HOUSE') NOT NULL DEFAULT 'APARTMENT'
        AFTER source_key,
    ADD KEY idx_public_data_raw_property_type (property_type, deal_date DESC);
