ALTER TABLE chore ADD COLUMN due_date DATE;
ALTER TABLE chore ADD COLUMN recurrence_freq VARCHAR(10);
ALTER TABLE chore ADD COLUMN recurrence_interval INTEGER;

CREATE TABLE chore_rotation_member (
    chore_id CHAR(36) NOT NULL,
    list_position INTEGER NOT NULL,
    member_id CHAR(36) NOT NULL,
    PRIMARY KEY (chore_id, list_position)
);

CREATE INDEX idx_chore_rotation_member_member ON chore_rotation_member(member_id);
