--liquibase formatted sql

--changeset roadmap:013-organisation-closed
-- Closing an Organisation (SPEC-organisation.md) keeps all its data, so it can be restored later.
ALTER TABLE organisation
    ADD COLUMN closed_at    DATETIME(6),
    ADD COLUMN closed_by_id BIGINT,
    ADD CONSTRAINT fk_organisation_closed_by FOREIGN KEY (closed_by_id) REFERENCES owner (id);
--rollback ALTER TABLE organisation DROP FOREIGN KEY fk_organisation_closed_by, DROP COLUMN closed_by_id, DROP COLUMN closed_at;
