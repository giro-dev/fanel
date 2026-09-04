CREATE TABLE event_publication (
    id BLOB NOT NULL PRIMARY KEY,
    listener_id VARCHAR(512) NOT NULL,
    event_type VARCHAR(512) NOT NULL,
    serialized_event TEXT NOT NULL,
    last_resubmission_date TIMESTAMP,
    publication_date TIMESTAMP NOT NULL,
    completion_date TIMESTAMP,
    completion_attempts INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL
);

CREATE INDEX idx_event_publication_completion ON event_publication(completion_date);
