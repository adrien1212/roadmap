--liquibase formatted sql

--changeset roadmap:012-invitation
-- A single-use link for one email and a Role (SPEC-organisation.md). Only the SHA-256 of the
-- link's secret is stored, compared byte for byte (ascii_bin), like public_id.
CREATE TABLE invitation (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    organisation_id BIGINT       NOT NULL,
    email           VARCHAR(254) NOT NULL,
    role            VARCHAR(16)  NOT NULL,
    invited_by_id   BIGINT       NOT NULL,
    secret_hash     CHAR(64)     CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_at      DATETIME(6)  NOT NULL,
    expires_at      DATETIME(6)  NOT NULL,
    used_at         DATETIME(6),
    revoked_at      DATETIME(6),
    CONSTRAINT fk_invitation_organisation FOREIGN KEY (organisation_id) REFERENCES organisation (id) ON DELETE CASCADE,
    CONSTRAINT fk_invitation_invited_by FOREIGN KEY (invited_by_id) REFERENCES owner (id),
    CONSTRAINT uk_invitation_secret_hash UNIQUE (secret_hash)
);
--rollback DROP TABLE invitation;
