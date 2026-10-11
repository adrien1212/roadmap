--liquibase formatted sql

--changeset roadmap:003-roadmap
-- public_id is compared byte for byte (ascii_bin): with the default case-insensitive
-- collation, ids differing only in case would match and lose entropy.
CREATE TABLE roadmap (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    owner_id     BIGINT       NOT NULL,
    name         VARCHAR(120) NOT NULL,
    public_id    VARCHAR(22)  CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    published_at DATETIME(6),
    CONSTRAINT fk_roadmap_owner FOREIGN KEY (owner_id) REFERENCES owner (id),
    CONSTRAINT uk_roadmap_public_id UNIQUE (public_id)
);
--rollback DROP TABLE roadmap;
