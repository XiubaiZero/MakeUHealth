CREATE TABLE IF NOT EXISTS assistant_preferences (
 account_id INT PRIMARY KEY,
 enter_send_enabled BOOLEAN NOT NULL DEFAULT TRUE,
 revision BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT fk_preferences_account FOREIGN KEY(account_id) REFERENCES auth_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
