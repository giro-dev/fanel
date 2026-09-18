ALTER TABLE member ADD COLUMN username VARCHAR(255);
ALTER TABLE member ADD COLUMN password_hash VARCHAR(255);

CREATE UNIQUE INDEX uq_member_username ON member(username);
