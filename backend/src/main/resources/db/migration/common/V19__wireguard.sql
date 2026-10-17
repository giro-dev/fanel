CREATE TABLE wg_peer (
    id CHAR(36) NOT NULL PRIMARY KEY,
    device_name VARCHAR(100) NOT NULL,
    public_key CHAR(44) NOT NULL UNIQUE,
    preshared_key VARCHAR(255),
    assigned_ip VARCHAR(15) NOT NULL UNIQUE,
    status VARCHAR(10) NOT NULL,
    owner_member_id CHAR(36),
    created_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    last_handshake_at TIMESTAMP
);

CREATE TABLE wg_pairing_token (
    id CHAR(36) NOT NULL PRIMARY KEY,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_by CHAR(36),
    created_at TIMESTAMP NOT NULL
);
