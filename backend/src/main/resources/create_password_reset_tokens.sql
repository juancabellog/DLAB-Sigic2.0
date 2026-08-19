-- Password reset tokens for forgot/reset password flow
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    used_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    created_ip VARCHAR(64) NULL,
    user_agent VARCHAR(512) NULL,
    CONSTRAINT uk_password_reset_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users_2_0 (id)
);

CREATE INDEX idx_password_reset_user_id ON password_reset_tokens (user_id);
