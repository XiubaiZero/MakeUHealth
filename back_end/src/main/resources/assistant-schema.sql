CREATE TABLE IF NOT EXISTS assistant_conversation (
  id CHAR(36) PRIMARY KEY,
  account_id INT NOT NULL,
  title VARCHAR(100) NULL,
  revision BIGINT NOT NULL DEFAULT 0,
  next_sequence BIGINT NOT NULL DEFAULT 1,
  import_fingerprint CHAR(64) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_assistant_import (account_id, import_fingerprint),
  KEY idx_assistant_account_updated (account_id, updated_at, id),
  CONSTRAINT fk_assistant_account FOREIGN KEY (account_id) REFERENCES auth_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS assistant_message (
  id CHAR(36) PRIMARY KEY,
  conversation_id CHAR(36) NOT NULL,
  sequence_no BIGINT NOT NULL,
  role VARCHAR(16) NOT NULL,
  content LONGTEXT NOT NULL,
  language VARCHAR(16) NULL,
  source VARCHAR(16) NOT NULL,
  suggestions_json TEXT NULL,
  created_at DATETIME(6) NULL,
  UNIQUE KEY uk_assistant_sequence (conversation_id, sequence_no),
  CONSTRAINT fk_assistant_message_conversation FOREIGN KEY (conversation_id) REFERENCES assistant_conversation(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS assistant_generation_task (
  id CHAR(36) PRIMARY KEY,
  conversation_id CHAR(36) NOT NULL,
  request_id CHAR(36) NOT NULL,
  question_id CHAR(36) NOT NULL,
  answer_id CHAR(36) NULL,
  status VARCHAR(16) NOT NULL,
  payload_json LONGTEXT NULL,
  error_message VARCHAR(200) NULL,
  created_at DATETIME(6) NOT NULL,
  started_at DATETIME(6) NULL,
  UNIQUE KEY uk_assistant_request (conversation_id, request_id),
  KEY idx_assistant_pending (status, started_at),
  CONSTRAINT fk_assistant_task_conversation FOREIGN KEY (conversation_id) REFERENCES assistant_conversation(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
