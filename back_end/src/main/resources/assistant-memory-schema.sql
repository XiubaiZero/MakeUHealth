CREATE TABLE IF NOT EXISTS assistant_memory_account (
 account_id INT PRIMARY KEY,
 enabled BOOLEAN NOT NULL DEFAULT TRUE,
 revision BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT fk_memory_account FOREIGN KEY(account_id) REFERENCES auth_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS assistant_memory_item (
 id CHAR(36) PRIMARY KEY,
 account_id INT NOT NULL,
 category VARCHAR(20) NOT NULL,
 content VARCHAR(800) NOT NULL,
 status VARCHAR(16) NOT NULL,
 revision BIGINT NOT NULL DEFAULT 0,
 normalized_hash CHAR(64) NOT NULL,
 source_conversation_id CHAR(36) NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_memory_item_account FOREIGN KEY(account_id) REFERENCES auth_user(id) ON DELETE CASCADE,
 CONSTRAINT fk_memory_item_conversation FOREIGN KEY(source_conversation_id) REFERENCES assistant_conversation(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS assistant_memory_source (
 memory_id CHAR(36) NOT NULL,
 message_id CHAR(36) NOT NULL,
 content_hash CHAR(64) NOT NULL,
 evidence TEXT NOT NULL,
 PRIMARY KEY(memory_id,message_id),
 CONSTRAINT fk_memory_source_item FOREIGN KEY(memory_id) REFERENCES assistant_memory_item(id) ON DELETE CASCADE,
 CONSTRAINT fk_memory_source_message FOREIGN KEY(message_id) REFERENCES assistant_message(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS assistant_memory_extraction (
 id CHAR(36) PRIMARY KEY,
 account_id INT NOT NULL,
 conversation_id CHAR(36) NOT NULL,
 request_id CHAR(36) NOT NULL,
 status VARCHAR(16) NOT NULL,
 payload_json LONGTEXT NULL,
 result_json LONGTEXT NULL,
 error_message VARCHAR(255) NULL,
 started_at TIMESTAMP NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(account_id,request_id),
 INDEX idx_extraction_status_created(status,created_at),
 CONSTRAINT fk_extraction_account FOREIGN KEY(account_id) REFERENCES auth_user(id) ON DELETE CASCADE,
 CONSTRAINT fk_extraction_conversation FOREIGN KEY(conversation_id) REFERENCES assistant_conversation(id) ON DELETE CASCADE
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
