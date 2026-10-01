-- LEGACY DEMO SNAPSHOT: drops tables. Never use this file to upgrade an existing database.
-- Normal setup/upgrade uses back_end/src/main/resources/schema.sql then data.sql.
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `weekly_progress`;
DROP TABLE IF EXISTS `fitness_goal`;
DROP TABLE IF EXISTS `food_intake`;
DROP TABLE IF EXISTS `food_library`;
DROP TABLE IF EXISTS `health_record`;
DROP TABLE IF EXISTS `reminder`;
DROP TABLE IF EXISTS `account_user_binding`;
DROP TABLE IF EXISTS `user`;
DROP TABLE IF EXISTS `auth_user`;

CREATE TABLE `user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `age` int NOT NULL,
  `gender` enum('male','female') COLLATE utf8mb4_unicode_ci NOT NULL,
  `height` double DEFAULT NULL,
  `weight` double DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `health_record` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `systolic` int DEFAULT NULL COMMENT 'Systolic blood pressure (mmHg)',
  `diastolic` int DEFAULT NULL COMMENT 'Diastolic blood pressure (mmHg)',
	  `fbg` double DEFAULT NULL,
	  `heart_rate` int DEFAULT NULL COMMENT 'Heart rate (bpm)',
	  `oxyhemoglobin` double DEFAULT NULL,
	  `age_snapshot` int DEFAULT NULL COMMENT 'Profile age snapshot',
	  `gender_snapshot` enum('male','female') COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Profile gender snapshot',
	  `height_snapshot` double DEFAULT NULL COMMENT 'Profile height snapshot (cm)',
	  `weight_snapshot` double DEFAULT NULL COMMENT 'Profile weight snapshot (kg)',
	  `recorded_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_health_record_user_id` (`user_id`),
  KEY `idx_health_record_recorded_at` (`recorded_at`),
  CONSTRAINT `fk_health_record_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `food_library` (
  `id` int NOT NULL AUTO_INCREMENT,
  `food_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `calories` int DEFAULT NULL,
  `nutrients` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `food_intake` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `food_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `amount` double NOT NULL,
  `unit` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `calories` int DEFAULT NULL,
  `nutrients` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `intake_time` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_food_intake_user_id` (`user_id`),
  KEY `idx_food_intake_time` (`intake_time`),
  CONSTRAINT `fk_food_intake_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `fitness_goal` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `goal_type` enum('fat_loss','muscle_gain','weight_loss') COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('active','archived','completed') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `current_value` decimal(6,2) NOT NULL,
  `target_value` decimal(6,2) NOT NULL,
  `target_date` date NOT NULL,
  `total_weeks` int DEFAULT NULL,
  `weekly_change` decimal(6,3) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) DEFAULT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_fitness_goal_user_id` (`user_id`),
  CONSTRAINT `fk_fitness_goal_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `weekly_progress` (
  `id` int NOT NULL AUTO_INCREMENT,
  `goal_id` int NOT NULL,
  `user_id` int NOT NULL,
  `current_value` decimal(6,2) NOT NULL,
  `is_on_track` bit(1) DEFAULT NULL,
  `progress_percentage` decimal(5,2) DEFAULT NULL,
  `week_start` date NOT NULL,
  `week_end` date NOT NULL,
  `weekly_change` decimal(6,3) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_weekly_progress_goal_id` (`goal_id`),
  KEY `idx_weekly_progress_user_id` (`user_id`),
  CONSTRAINT `fk_weekly_progress_goal` FOREIGN KEY (`goal_id`) REFERENCES `fitness_goal` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_weekly_progress_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `auth_user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `account` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `account_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_user_account` (`account`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `account_user_binding` (
  `account_id` int NOT NULL,
  `user_id` int NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`account_id`),
  UNIQUE KEY `uk_account_user_binding_user_id` (`user_id`),
  CONSTRAINT `fk_account_user_binding_account` FOREIGN KEY (`account_id`) REFERENCES `auth_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_account_user_binding_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `reminder` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_id` int NOT NULL,
  `user_id` int NOT NULL,
  `reminder_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reminder_time` datetime NOT NULL,
  `repeat_pattern` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT 'none',
  `note` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_reminder_account_user` (`account_id`, `user_id`),
  KEY `idx_reminder_time` (`reminder_time`),
  CONSTRAINT `fk_reminder_account` FOREIGN KEY (`account_id`) REFERENCES `auth_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `user` (`id`, `age`, `gender`, `height`, `weight`, `created_at`, `updated_at`) VALUES
(1, 21, 'female', 162, 55, '2026-03-31 11:05:26.550993', '2026-03-31 11:05:26.550993'),
(2, 20, 'male', 185, 75, '2026-03-31 11:06:03.883386', '2026-03-31 11:06:03.883386'),
(3, 20, 'male', 180, 75, '2026-03-31 11:20:20.231651', '2026-03-31 11:20:20.231651'),
(4, 30, 'male', 175.5, 70.2, '2026-03-24 19:43:30.000000', '2026-03-24 19:43:30.000000'),
(5, 20, 'male', 167, 65, '2026-04-07 19:43:33.000000', '2026-04-07 19:43:33.000000');

INSERT INTO `health_record` (`id`, `user_id`, `systolic`, `diastolic`, `fbg`, `heart_rate`, `oxyhemoglobin`, `recorded_at`) VALUES
(1, 3, 120, 80, 5.6, 78, 98, '2026-03-31 11:20:29.750169');

INSERT INTO `food_library` (`id`, `food_name`, `calories`, `nutrients`) VALUES
(1, 'Rice', 116, 'Carbohydrate'),
(2, 'Apple', 52, 'Vitamin C, Fiber'),
(3, 'Egg', 155, 'Protein'),
(4, 'Chicken Breast', 165, 'Protein'),
(5, 'Banana', 96, 'Potassium, Vitamin B6'),
(6, 'Milk', 42, 'Calcium, Protein');

INSERT INTO `food_intake` (`id`, `user_id`, `food_name`, `amount`, `unit`, `calories`, `nutrients`, `intake_time`) VALUES
(1, 3, 'Rice', 100, 'g', 220, 'Carbohydrate', '2026-03-31 11:21:15.826945');

INSERT INTO `fitness_goal` (`id`, `user_id`, `goal_type`, `status`, `current_value`, `target_value`, `target_date`, `total_weeks`, `weekly_change`, `created_at`, `updated_at`, `completed_at`) VALUES
(20, 4, 'weight_loss', 'completed', 60.00, 50.00, '2026-04-18', 3, -3.333, '2026-03-29 16:01:48.000000', NULL, '2026-03-29 16:05:37.373915'),
(21, 4, 'fat_loss', 'completed', 30.00, 28.00, '2026-04-25', 4, -0.500, '2026-03-29 16:06:38.000000', NULL, '2026-03-29 16:06:51.713989'),
(22, 4, 'weight_loss', 'active', 60.00, 57.00, '2026-04-26', 4, -0.750, '2026-03-29 16:07:08.000000', NULL, NULL);

INSERT INTO `weekly_progress` (`id`, `goal_id`, `user_id`, `current_value`, `is_on_track`, `progress_percentage`, `week_start`, `week_end`, `weekly_change`, `created_at`) VALUES
(11, 20, 4, 50.00, b'1', 100.00, '2026-03-23', '2026-03-29', -10.000, '2026-03-29 16:05:37.000000'),
(12, 21, 4, 28.00, b'1', 100.00, '2026-03-23', '2026-03-29', -2.000, '2026-03-29 16:06:51.000000'),
(13, 22, 4, 58.00, b'1', 66.67, '2026-03-23', '2026-03-29', -2.000, '2026-03-29 16:07:17.000000'),
(14, 22, 4, 57.50, b'0', 83.33, '2026-04-06', '2026-04-12', -0.500, '2026-04-07 17:03:21.000000');

SET FOREIGN_KEY_CHECKS = 1;
