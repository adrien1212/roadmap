--liquibase formatted sql

--changeset roadmap:002-category
-- Names are unique per Owner ignoring case (and accents): the default collation is *_ai_ci.
CREATE TABLE category (
    id       BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT      NOT NULL,
    name     VARCHAR(60) NOT NULL,
    colour   VARCHAR(16) NOT NULL,
    CONSTRAINT fk_category_owner FOREIGN KEY (owner_id) REFERENCES owner (id),
    CONSTRAINT uk_category_owner_name UNIQUE (owner_id, name)
);
--rollback DROP TABLE category;
