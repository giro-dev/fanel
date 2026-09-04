CREATE TABLE household (
    id CHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    locale VARCHAR(32) NOT NULL,
    timezone VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE member (
    id CHAR(36) NOT NULL PRIMARY KEY,
    household_id CHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(16) NOT NULL,
    color VARCHAR(32),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_member_household FOREIGN KEY (household_id) REFERENCES household(id)
);

CREATE INDEX idx_member_household ON member(household_id);
