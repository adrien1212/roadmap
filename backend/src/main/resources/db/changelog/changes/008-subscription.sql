--liquibase formatted sql

--changeset roadmap:008-subscription
-- A Reader's email on a Roadmap. Deleted on unsubscribe; only roadmap.unsubscribed_count remains.
-- The token is compared byte for byte (ascii_bin), like public_id.
CREATE TABLE subscription (
    id                   BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    roadmap_id           BIGINT       NOT NULL,
    email                VARCHAR(254) NOT NULL,
    state                VARCHAR(16)  NOT NULL,
    token                VARCHAR(22)  CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_at           DATETIME(6)  NOT NULL,
    confirmed_at         DATETIME(6),
    confirmation_sent_at DATETIME(6),
    CONSTRAINT fk_subscription_roadmap FOREIGN KEY (roadmap_id) REFERENCES roadmap (id) ON DELETE CASCADE,
    CONSTRAINT uk_subscription_roadmap_email UNIQUE (roadmap_id, email),
    CONSTRAINT uk_subscription_token UNIQUE (token)
);
ALTER TABLE roadmap ADD COLUMN unsubscribed_count INT NOT NULL DEFAULT 0;
--rollback ALTER TABLE roadmap DROP COLUMN unsubscribed_count;
--rollback DROP TABLE subscription;
