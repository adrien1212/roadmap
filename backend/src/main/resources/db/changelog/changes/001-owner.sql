--liquibase formatted sql

--changeset roadmap:001-owner
CREATE TABLE owner (
    id               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    provider         VARCHAR(32)  NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    email            VARCHAR(320),
    name             VARCHAR(255),
    created_at       DATETIME(6)  NOT NULL,
    CONSTRAINT uk_owner_provider_subject UNIQUE (provider, provider_subject)
);
--rollback DROP TABLE owner;
