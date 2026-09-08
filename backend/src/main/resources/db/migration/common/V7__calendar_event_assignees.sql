CREATE TABLE calendar_event_assignee (
    event_id CHAR(36) NOT NULL,
    member_id CHAR(36) NOT NULL,
    PRIMARY KEY (event_id, member_id)
);

CREATE INDEX idx_calendar_event_assignee_member ON calendar_event_assignee(member_id);
