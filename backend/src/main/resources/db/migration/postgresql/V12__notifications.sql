CREATE TABLE vapid_key_pair (
    id CHAR(36) NOT NULL PRIMARY KEY,
    x509_public_key VARCHAR(512) NOT NULL,
    pkcs8_private_key VARCHAR(512) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE push_subscription (
    id CHAR(36) NOT NULL PRIMARY KEY,
    household_id CHAR(36) NOT NULL,
    member_id CHAR(36) NOT NULL,
    endpoint VARCHAR(2048) NOT NULL,
    p256dh VARCHAR(255) NOT NULL,
    auth VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_push_subscription_household FOREIGN KEY (household_id) REFERENCES household(id)
);

CREATE UNIQUE INDEX uq_push_subscription_endpoint ON push_subscription(endpoint);
CREATE INDEX idx_push_subscription_household_member ON push_subscription(household_id, member_id);
