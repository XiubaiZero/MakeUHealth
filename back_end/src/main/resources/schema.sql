CREATE TABLE IF NOT EXISTS `food_library` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `food_name` VARCHAR(100) NOT NULL,
  `calories` INT NOT NULL,
  `nutrients` VARCHAR(255) DEFAULT NULL,
  `protein` DOUBLE DEFAULT NULL,
  `carbs` DOUBLE DEFAULT NULL,
  `fat` DOUBLE DEFAULT NULL,
  `fiber` DOUBLE DEFAULT NULL,
  `sodium` DOUBLE DEFAULT NULL,
  `sugar` DOUBLE DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_food_library_food_name` (`food_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Upgrade existing food libraries before data.sql is executed. Values are preserved.
SET @sql_add_protein = (
  SELECT IF(
    EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'food_library' AND COLUMN_NAME = 'protein'),
    'SELECT 1',
    'ALTER TABLE `food_library` ADD COLUMN `protein` DOUBLE DEFAULT NULL'
  )
);
PREPARE stmt FROM @sql_add_protein;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql_add_carbs = (
  SELECT IF(
    EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'food_library' AND COLUMN_NAME = 'carbs'),
    'SELECT 1',
    'ALTER TABLE `food_library` ADD COLUMN `carbs` DOUBLE DEFAULT NULL'
  )
);
PREPARE stmt FROM @sql_add_carbs;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql_add_fat = (
  SELECT IF(
    EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'food_library' AND COLUMN_NAME = 'fat'),
    'SELECT 1',
    'ALTER TABLE `food_library` ADD COLUMN `fat` DOUBLE DEFAULT NULL'
  )
);
PREPARE stmt FROM @sql_add_fat;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql_add_fiber = (
  SELECT IF(
    EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'food_library' AND COLUMN_NAME = 'fiber'),
    'SELECT 1',
    'ALTER TABLE `food_library` ADD COLUMN `fiber` DOUBLE DEFAULT NULL'
  )
);
PREPARE stmt FROM @sql_add_fiber;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql_add_sodium = (
  SELECT IF(
    EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'food_library' AND COLUMN_NAME = 'sodium'),
    'SELECT 1',
    'ALTER TABLE `food_library` ADD COLUMN `sodium` DOUBLE DEFAULT NULL'
  )
);
PREPARE stmt FROM @sql_add_sodium;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql_add_sugar = (
  SELECT IF(
    EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'food_library' AND COLUMN_NAME = 'sugar'),
    'SELECT 1',
    'ALTER TABLE `food_library` ADD COLUMN `sugar` DOUBLE DEFAULT NULL'
  )
);
PREPARE stmt FROM @sql_add_sugar;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `food_intake` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `food_name` VARCHAR(100) NOT NULL,
  `amount` DOUBLE NOT NULL,
  `unit` VARCHAR(50) DEFAULT NULL,
  `calories` INT DEFAULT NULL,
  `nutrients` VARCHAR(255) DEFAULT NULL,
  `intake_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `meal_type` VARCHAR(20) DEFAULT 'snack',
  PRIMARY KEY (`id`),
  KEY `idx_food_intake_user_id` (`user_id`),
  KEY `idx_food_intake_time` (`intake_time`),
  KEY `idx_food_intake_meal_type` (`meal_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @sql_add_meal_type = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'food_intake'
        AND COLUMN_NAME = 'meal_type'
    ),
    'SELECT 1',
    'ALTER TABLE `food_intake` ADD COLUMN `meal_type` VARCHAR(20) DEFAULT ''snack'''
  )
);
PREPARE stmt FROM @sql_add_meal_type;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql_add_meal_type_index = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM INFORMATION_SCHEMA.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'food_intake'
        AND INDEX_NAME = 'idx_food_intake_meal_type'
    ),
    'SELECT 1',
    'CREATE INDEX `idx_food_intake_meal_type` ON `food_intake`(`meal_type`)'
  )
);
PREPARE stmt FROM @sql_add_meal_type_index;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `daily_meal_target` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `target_date` DATE NOT NULL,
  `breakfast_target` INT DEFAULT 0,
  `lunch_target` INT DEFAULT 0,
  `dinner_target` INT DEFAULT 0,
  `snack_target` INT DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_daily_meal_target_user_date` (`user_id`, `target_date`),
  KEY `idx_daily_meal_target_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `auth_user` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `account` VARCHAR(255) NOT NULL,
  `account_type` VARCHAR(20) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_user_account` (`account`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `reminder` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `account_id` INT NOT NULL,
  `user_id` INT NOT NULL,
  `reminder_type` VARCHAR(50) NOT NULL,
  `reminder_time` DATETIME NOT NULL,
  `repeat_pattern` VARCHAR(30) DEFAULT 'none',
  `note` VARCHAR(255) DEFAULT NULL,
  `enabled` TINYINT(1) NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_reminder_account_user` (`account_id`, `user_id`),
  KEY `idx_reminder_time` (`reminder_time`),
  CONSTRAINT `fk_reminder_account` FOREIGN KEY (`account_id`) REFERENCES `auth_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `age` INT NOT NULL,
  `gender` ENUM('male','female') NOT NULL,
  `height` DOUBLE DEFAULT NULL,
  `weight` DOUBLE DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `account_user_binding` (
  `account_id` INT NOT NULL,
  `user_id` INT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`account_id`),
  UNIQUE KEY `uk_account_user_binding_user_id` (`user_id`),
  CONSTRAINT `fk_account_user_binding_account` FOREIGN KEY (`account_id`) REFERENCES `auth_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_account_user_binding_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `health_record` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `systolic` INT DEFAULT NULL,
  `diastolic` INT DEFAULT NULL,
  `fbg` DOUBLE DEFAULT NULL,
  `heart_rate` INT DEFAULT NULL,
  `oxyhemoglobin` DOUBLE DEFAULT NULL,
  `age_snapshot` INT DEFAULT NULL,
  `gender_snapshot` ENUM('male','female') DEFAULT NULL,
  `height_snapshot` DOUBLE DEFAULT NULL,
  `weight_snapshot` DOUBLE DEFAULT NULL,
  `recorded_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_health_record_user_id` (`user_id`),
  KEY `idx_health_record_recorded_at` (`recorded_at`),
  CONSTRAINT `fk_health_record_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @sql_add_age_snapshot = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'health_record'
        AND COLUMN_NAME = 'age_snapshot'
    ),
    'SELECT 1',
    'ALTER TABLE `health_record` ADD COLUMN `age_snapshot` INT DEFAULT NULL'
  )
);
PREPARE stmt FROM @sql_add_age_snapshot;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql_add_gender_snapshot = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'health_record'
        AND COLUMN_NAME = 'gender_snapshot'
    ),
    'SELECT 1',
    'ALTER TABLE `health_record` ADD COLUMN `gender_snapshot` ENUM(''male'',''female'') DEFAULT NULL'
  )
);
PREPARE stmt FROM @sql_add_gender_snapshot;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql_add_height_snapshot = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'health_record'
        AND COLUMN_NAME = 'height_snapshot'
    ),
    'SELECT 1',
    'ALTER TABLE `health_record` ADD COLUMN `height_snapshot` DOUBLE DEFAULT NULL'
  )
);
PREPARE stmt FROM @sql_add_height_snapshot;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql_add_weight_snapshot = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'health_record'
        AND COLUMN_NAME = 'weight_snapshot'
    ),
    'SELECT 1',
    'ALTER TABLE `health_record` ADD COLUMN `weight_snapshot` DOUBLE DEFAULT NULL'
  )
);
PREPARE stmt FROM @sql_add_weight_snapshot;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `health_record` hr
LEFT JOIN `user` u ON u.`id` = hr.`user_id`
SET
  hr.`age_snapshot` = COALESCE(hr.`age_snapshot`, u.`age`),
  hr.`gender_snapshot` = COALESCE(hr.`gender_snapshot`, u.`gender`),
  hr.`height_snapshot` = COALESCE(hr.`height_snapshot`, u.`height`),
  hr.`weight_snapshot` = COALESCE(hr.`weight_snapshot`, u.`weight`);

CREATE TABLE IF NOT EXISTS `fitness_goal` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `goal_type` ENUM('fat_loss','muscle_gain','weight_loss') NOT NULL,
  `status` ENUM('active','archived','completed') DEFAULT 'active',
  `current_value` DECIMAL(6,2) NOT NULL,
  `target_value` DECIMAL(6,2) NOT NULL,
  `target_date` DATE NOT NULL,
  `total_weeks` INT DEFAULT NULL,
  `weekly_change` DECIMAL(6,3) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `completed_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_fitness_goal_user_id` (`user_id`),
  CONSTRAINT `fk_fitness_goal_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `weekly_progress` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `goal_id` INT NOT NULL,
  `user_id` INT NOT NULL,
  `current_value` DECIMAL(6,2) NOT NULL,
  `is_on_track` BIT(1) DEFAULT NULL,
  `progress_percentage` DECIMAL(5,2) DEFAULT NULL,
  `week_start` DATE NOT NULL,
  `week_end` DATE NOT NULL,
  `weekly_change` DECIMAL(6,3) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_weekly_progress_goal_id` (`goal_id`),
  KEY `idx_weekly_progress_user_id` (`user_id`),
  CONSTRAINT `fk_weekly_progress_goal` FOREIGN KEY (`goal_id`) REFERENCES `fitness_goal` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_weekly_progress_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
