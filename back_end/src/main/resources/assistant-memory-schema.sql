CREATE TABLE IF NOT EXISTS assistant_memory_account (
 account_id INT PRIMARY KEY,
 enabled BOOLEAN NOT NULL DEFAULT TRUE,
 revision BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT fk_memory_account FOREIGN KEY(account_id) REFERENCES auth_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS assistant_memory_message (
 message_id CHAR(36) PRIMARY KEY,
 metadata_json LONGTEXT NOT NULL,
 CONSTRAINT fk_memory_message FOREIGN KEY(message_id) REFERENCES assistant_message(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS assistant_memory_conversation (
 conversation_id CHAR(36) PRIMARY KEY,
 enabled BOOLEAN NOT NULL DEFAULT TRUE,
 revision BIGINT NOT NULL DEFAULT 0,
 content_revision BIGINT NOT NULL DEFAULT 0,
 summary LONGTEXT NULL,
 covered_sequence BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT fk_memory_conversation FOREIGN KEY(conversation_id) REFERENCES assistant_conversation(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
