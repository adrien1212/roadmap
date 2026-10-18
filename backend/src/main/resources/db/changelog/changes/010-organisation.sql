--liquibase formatted sql

--changeset roadmap:010-organisation
-- Expand (SPEC-organisation.md): each existing Owner gets an Organisation with the same id,
-- as its Role Owner, so organisation_id = owner_id everywhere. The owner_id columns and
-- owner.brand_name are dropped by 011 once the code no longer uses them.
CREATE TABLE organisation (
    id         BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(80) NOT NULL,
    brand_name VARCHAR(60),
    created_at DATETIME(6) NOT NULL
);
INSERT INTO organisation (id, name, brand_name, created_at)
SELECT id,
       CASE WHEN name IS NULL OR TRIM(name) = '' THEN 'My Organisation'
            ELSE CONCAT(LEFT(TRIM(name), 65), '''s Organisation') END,
       brand_name,
       created_at
FROM owner;

ALTER TABLE owner
    ADD COLUMN organisation_id BIGINT,
    ADD COLUMN role            VARCHAR(16),
    ADD COLUMN joined_at       DATETIME(6),
    ADD COLUMN email_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD CONSTRAINT fk_owner_organisation FOREIGN KEY (organisation_id) REFERENCES organisation (id);
UPDATE owner SET organisation_id = id, role = 'OWNER', joined_at = created_at;

ALTER TABLE roadmap
    ADD COLUMN organisation_id BIGINT,
    ADD CONSTRAINT fk_roadmap_organisation FOREIGN KEY (organisation_id) REFERENCES organisation (id);
UPDATE roadmap SET organisation_id = owner_id;

ALTER TABLE category
    ADD COLUMN organisation_id BIGINT,
    ADD CONSTRAINT fk_category_organisation FOREIGN KEY (organisation_id) REFERENCES organisation (id);
UPDATE category SET organisation_id = owner_id;

ALTER TABLE smtp_settings
    ADD COLUMN organisation_id BIGINT,
    ADD CONSTRAINT fk_smtp_settings_organisation FOREIGN KEY (organisation_id) REFERENCES organisation (id);
UPDATE smtp_settings SET organisation_id = owner_id;
--rollback ALTER TABLE smtp_settings DROP FOREIGN KEY fk_smtp_settings_organisation, DROP COLUMN organisation_id;
--rollback ALTER TABLE category DROP FOREIGN KEY fk_category_organisation, DROP COLUMN organisation_id;
--rollback ALTER TABLE roadmap DROP FOREIGN KEY fk_roadmap_organisation, DROP COLUMN organisation_id;
--rollback ALTER TABLE owner DROP FOREIGN KEY fk_owner_organisation, DROP COLUMN organisation_id, DROP COLUMN role, DROP COLUMN joined_at, DROP COLUMN email_verified;
--rollback DROP TABLE organisation;
