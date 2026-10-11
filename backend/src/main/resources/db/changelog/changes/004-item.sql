--liquibase formatted sql

--changeset roadmap:004-item
-- Deleting a Roadmap deletes its Items; deleting a Category only clears it on Items.
CREATE TABLE item (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    roadmap_id  BIGINT       NOT NULL,
    category_id BIGINT,
    title       VARCHAR(140) NOT NULL,
    description TEXT,
    start_date  DATE,
    end_date    DATE,
    status      VARCHAR(16)  NOT NULL,
    internal    BOOLEAN      NOT NULL DEFAULT FALSE,
    position    INT          NOT NULL,
    CONSTRAINT fk_item_roadmap FOREIGN KEY (roadmap_id) REFERENCES roadmap (id) ON DELETE CASCADE,
    CONSTRAINT fk_item_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE SET NULL,
    INDEX idx_item_roadmap_position (roadmap_id, position)
);
--rollback DROP TABLE item;
