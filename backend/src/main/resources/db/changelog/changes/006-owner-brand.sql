--liquibase formatted sql

--changeset roadmap:006-owner-brand
-- The Owner's Brand: the product name Readers see. Optional; never filled from the Google profile.
ALTER TABLE owner ADD COLUMN brand_name VARCHAR(60);
--rollback ALTER TABLE owner DROP COLUMN brand_name;
