CREATE TABLE calendar_event_assignee (
    event_id CHAR(36) NOT NULL,
    member_id CHAR(36) NOT NULL,
    CONSTRAINT fk_calendar_event_assignee_event FOREIGN KEY (event_id) REFERENCES calendar_event(id)
);

CREATE INDEX idx_calendar_event_assignee_event ON calendar_event_assignee(event_id);
