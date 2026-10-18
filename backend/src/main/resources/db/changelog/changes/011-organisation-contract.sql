--liquibase formatted sql

--changeset roadmap:011-organisation-contract
-- Contract (SPEC-organisation.md): Roadmaps, Categories, the Brand and SMTP settings now belong
-- to the Organisation only. Dropping the owner_id columns makes any query left on them fail loudly.
ALTER TABLE roadmap DROP FOREIGN KEY fk_roadmap_owner;
ALTER TABLE roadmap DROP COLUMN owner_id, MODIFY organisation_id BIGINT NOT NULL;

ALTER TABLE category DROP FOREIGN KEY fk_category_owner;
-- Names stay unique ignoring case (and accents) through the *_ai_ci collation, now per Organisation.
ALTER TABLE category
    DROP INDEX uk_category_owner_name,
    DROP COLUMN owner_id,
    MODIFY organisation_id BIGINT NOT NULL,
    ADD CONSTRAINT uk_category_organisation_name UNIQUE (organisation_id, name);

ALTER TABLE smtp_settings DROP FOREIGN KEY fk_smtp_settings_owner;
ALTER TABLE smtp_settings
    DROP PRIMARY KEY,
    DROP COLUMN owner_id,
    MODIFY organisation_id BIGINT NOT NULL,
    ADD PRIMARY KEY (organisation_id);

ALTER TABLE owner DROP COLUMN brand_name;
-- Rollback gives each row back to its Organisation's Role Owner, the one Owner every Organisation has.
--rollback ALTER TABLE owner ADD COLUMN brand_name VARCHAR(60);
--rollback UPDATE owner o JOIN organisation g ON g.id = o.organisation_id SET o.brand_name = g.brand_name WHERE o.role = 'OWNER';
--rollback ALTER TABLE smtp_settings DROP PRIMARY KEY, ADD COLUMN owner_id BIGINT, MODIFY organisation_id BIGINT NULL;
--rollback UPDATE smtp_settings s JOIN owner o ON o.organisation_id = s.organisation_id AND o.role = 'OWNER' SET s.owner_id = o.id;
--rollback ALTER TABLE smtp_settings MODIFY owner_id BIGINT NOT NULL, ADD PRIMARY KEY (owner_id), ADD CONSTRAINT fk_smtp_settings_owner FOREIGN KEY (owner_id) REFERENCES owner (id);
--rollback ALTER TABLE category DROP INDEX uk_category_organisation_name, ADD COLUMN owner_id BIGINT, MODIFY organisation_id BIGINT NULL;
--rollback UPDATE category c JOIN owner o ON o.organisation_id = c.organisation_id AND o.role = 'OWNER' SET c.owner_id = o.id;
--rollback ALTER TABLE category MODIFY owner_id BIGINT NOT NULL, ADD CONSTRAINT fk_category_owner FOREIGN KEY (owner_id) REFERENCES owner (id), ADD CONSTRAINT uk_category_owner_name UNIQUE (owner_id, name);
--rollback ALTER TABLE roadmap ADD COLUMN owner_id BIGINT, MODIFY organisation_id BIGINT NULL;
--rollback UPDATE roadmap r JOIN owner o ON o.organisation_id = r.organisation_id AND o.role = 'OWNER' SET r.owner_id = o.id;
--rollback ALTER TABLE roadmap MODIFY owner_id BIGINT NOT NULL, ADD CONSTRAINT fk_roadmap_owner FOREIGN KEY (owner_id) REFERENCES owner (id);
