--liquibase formatted sql

--changeset roadmap:015-item-subscription
-- An Item Subscription covers one Item (SPEC-item-updates.md); item_id is null for a Roadmap Subscription.
-- NULLs never clash in a unique key, so item_key (0 for the Roadmap) keeps one Subscription per address
-- on the Roadmap and one per address and Item. The new key is added before the old one is dropped:
-- its leading roadmap_id keeps backing fk_subscription_roadmap.
ALTER TABLE subscription
    ADD COLUMN item_id  BIGINT,
    ADD COLUMN item_key BIGINT AS (COALESCE(item_id, 0)) STORED,
    ADD CONSTRAINT fk_subscription_item FOREIGN KEY (item_id) REFERENCES item (id) ON DELETE CASCADE,
    ADD CONSTRAINT uk_subscription_roadmap_item_email UNIQUE (roadmap_id, item_key, email);
ALTER TABLE subscription DROP INDEX uk_subscription_roadmap_email;
--rollback DELETE FROM subscription WHERE item_id IS NOT NULL;
--rollback ALTER TABLE subscription ADD CONSTRAINT uk_subscription_roadmap_email UNIQUE (roadmap_id, email);
--rollback ALTER TABLE subscription DROP INDEX uk_subscription_roadmap_item_email, DROP FOREIGN KEY fk_subscription_item, DROP COLUMN item_key, DROP COLUMN item_id;
