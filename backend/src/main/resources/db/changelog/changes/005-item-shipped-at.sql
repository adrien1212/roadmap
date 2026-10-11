--liquibase formatted sql

--changeset roadmap:005-item-shipped-at
-- When the Status last became Shipped. Items shipped before this change keep NULL: no date is invented.
ALTER TABLE item ADD COLUMN shipped_at DATETIME(6);
--rollback ALTER TABLE item DROP COLUMN shipped_at;
