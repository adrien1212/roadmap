--liquibase formatted sql

--changeset roadmap:009-drop-unsubscribed-count
-- Unsubscribed Readers are no longer counted: unsubscribing just deletes the Subscription.
ALTER TABLE roadmap DROP COLUMN unsubscribed_count;
--rollback ALTER TABLE roadmap ADD COLUMN unsubscribed_count INT NOT NULL DEFAULT 0;
