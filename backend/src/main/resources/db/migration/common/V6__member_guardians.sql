CREATE TABLE member_guardian (
    child_id CHAR(36) NOT NULL,
    guardian_id CHAR(36) NOT NULL,
    PRIMARY KEY (child_id, guardian_id),
    CONSTRAINT fk_member_guardian_child FOREIGN KEY (child_id) REFERENCES member(id),
    CONSTRAINT fk_member_guardian_guardian FOREIGN KEY (guardian_id) REFERENCES member(id)
);

CREATE INDEX idx_member_guardian_guardian ON member_guardian(guardian_id);
