--liquibase formatted sql

--changeset roadmap:014-item-date-precision
-- How exact each date is (SPEC-fuzzy-dates.md). The date columns keep the first day of the period.
-- Existing dates were all days.
ALTER TABLE item
    ADD COLUMN start_precision VARCHAR(8) NOT NULL DEFAULT 'DAY',
    ADD COLUMN end_precision   VARCHAR(8) NOT NULL DEFAULT 'DAY';
--rollback ALTER TABLE item DROP COLUMN end_precision, DROP COLUMN start_precision;
