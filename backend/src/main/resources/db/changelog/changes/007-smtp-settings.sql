--liquibase formatted sql

--changeset roadmap:007-smtp-settings
-- The Owner's SMTP server for emails to subscribed Readers. The password is AES-GCM sealed (SMTP_SECRET_KEY).
CREATE TABLE smtp_settings (
    owner_id           BIGINT       NOT NULL PRIMARY KEY,
    host               VARCHAR(253) NOT NULL,
    port               INT          NOT NULL,
    security           VARCHAR(16)  NOT NULL,
    username           VARCHAR(255),
    password_encrypted VARBINARY(512),
    from_name          VARCHAR(100),
    from_email         VARCHAR(254) NOT NULL,
    state              VARCHAR(16)  NOT NULL,
    last_error         VARCHAR(500),
    tested_at          DATETIME(6),
    CONSTRAINT fk_smtp_settings_owner FOREIGN KEY (owner_id) REFERENCES owner (id)
);
--rollback DROP TABLE smtp_settings;
