/*
 Navicat Premium Data Transfer

 Source Server         : 11
 Source Server Type    : MySQL
 Source Server Version : 80019
 Source Host           : localhost:3306
 Source Schema         : ipd

 Target Server Type    : MySQL
 Target Server Version : 80019
 File Encoding         : 65001

 Date: 07/04/2026 19:49:18
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for fitness_goal
-- ----------------------------
DROP TABLE IF EXISTS `fitness_goal`;
CREATE TABLE `fitness_goal`  (
  `id` int(0) NOT NULL AUTO_INCREMENT,
  `completed_at` datetime(6) NULL DEFAULT NULL,
  `created_at` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `current_value` decimal(6, 2) NOT NULL,
  `goal_type` enum('fat_loss','muscle_gain','weight_loss') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `status` enum('active','archived','completed') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `target_date` date NOT NULL,
  `target_value` decimal(6, 2) NOT NULL,
  `total_weeks` int(0) NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `weekly_change` decimal(6, 3) NULL DEFAULT NULL,
  `user_id` int(0) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FKqhf8mmd2g2t0qhnf4bnjs2mcu`(`user_id`) USING BTREE,
  CONSTRAINT `FKqhf8mmd2g2t0qhnf4bnjs2mcu` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 23 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of fitness_goal
-- ----------------------------
INSERT INTO `fitness_goal` VALUES (20, '2026-03-29 16:05:37.373915', '2026-03-29 16:01:48', 60.00, 'weight_loss', 'completed', '2026-04-18', 50.00, 3, NULL, -3.333, 1);
INSERT INTO `fitness_goal` VALUES (21, '2026-03-29 16:06:51.713989', '2026-03-29 16:06:38', 30.00, 'fat_loss', 'completed', '2026-04-25', 28.00, 4, NULL, -0.500, 1);
INSERT INTO `fitness_goal` VALUES (22, NULL, '2026-03-29 16:07:08', 60.00, 'weight_loss', 'active', '2026-04-26', 57.00, 4, NULL, -0.750, 1);

-- ----------------------------
-- Table structure for health_record
-- ----------------------------
DROP TABLE IF EXISTS `health_record`;
CREATE TABLE `health_record`  (
  `id` int(0) NOT NULL AUTO_INCREMENT,
  `user_id` int(0) NOT NULL,
  `systolic` int(0) NULL DEFAULT NULL COMMENT 'Systolic blood pressure (mmHg)',
  `diastolic` int(0) NULL DEFAULT NULL COMMENT 'Diastolic blood pressure (mmHg)',
	  `fbg` double NULL DEFAULT NULL,
	  `heart_rate` int(0) NULL DEFAULT NULL COMMENT 'Heart rate (bpm)',
	  `oxyhemoglobin` double NULL DEFAULT NULL,
	  `age_snapshot` int(0) NULL DEFAULT NULL COMMENT 'Profile age snapshot',
	  `gender_snapshot` enum('male','female') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'Profile gender snapshot',
	  `height_snapshot` double NULL DEFAULT NULL COMMENT 'Profile height snapshot (cm)',
	  `weight_snapshot` double NULL DEFAULT NULL COMMENT 'Profile weight snapshot (kg)',
	  `recorded_at` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_recorded_at`(`recorded_at`) USING BTREE,
  CONSTRAINT `health_record_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of health_record
-- ----------------------------

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` int(0) NOT NULL AUTO_INCREMENT,
  `account_id` int(0) NULL DEFAULT NULL,
  `age` int(0) NOT NULL,
  `gender` enum('male','female') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `height` double NULL DEFAULT NULL,
  `weight` double NULL DEFAULT NULL,
  `created_at` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_account_id`(`account_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1, 1, 30, 'male', 175.5, 70.2, '2026-03-24 19:43:30', '2026-03-24 19:43:30');
INSERT INTO `user` VALUES (4, 1, 20, 'male', 167, 65, '2026-04-07 19:43:33', '2026-04-07 19:43:33');

-- ----------------------------
-- Table structure for weekly_progress
-- ----------------------------
DROP TABLE IF EXISTS `weekly_progress`;
CREATE TABLE `weekly_progress`  (
  `id` int(0) NOT NULL AUTO_INCREMENT,
  `created_at` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `current_value` decimal(6, 2) NOT NULL,
  `is_on_track` bit(1) NULL DEFAULT NULL,
  `progress_percentage` decimal(5, 2) NULL DEFAULT NULL,
  `week_end` date NOT NULL,
  `week_start` date NOT NULL,
  `weekly_change` decimal(6, 3) NULL DEFAULT NULL,
  `goal_id` int(0) NOT NULL,
  `user_id` int(0) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FKmutlxen17ohs6af29dskt9opr`(`goal_id`) USING BTREE,
  INDEX `FKckawwc2sd5eslof844tqmhul7`(`user_id`) USING BTREE,
  CONSTRAINT `FKckawwc2sd5eslof844tqmhul7` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `FKmutlxen17ohs6af29dskt9opr` FOREIGN KEY (`goal_id`) REFERENCES `fitness_goal` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of weekly_progress
-- ----------------------------
INSERT INTO `weekly_progress` VALUES (11, '2026-03-29 16:05:37', 50.00, b'1', 100.00, '2026-03-29', '2026-03-23', -10.000, 20, 1);
INSERT INTO `weekly_progress` VALUES (12, '2026-03-29 16:06:51', 28.00, b'1', 100.00, '2026-03-29', '2026-03-23', -2.000, 21, 1);
INSERT INTO `weekly_progress` VALUES (13, '2026-03-29 16:07:17', 58.00, b'1', 66.67, '2026-03-29', '2026-03-23', -2.000, 22, 1);
INSERT INTO `weekly_progress` VALUES (14, '2026-04-07 17:03:21', 57.50, b'0', 83.33, '2026-04-12', '2026-04-06', -0.500, 22, 1);

SET FOREIGN_KEY_CHECKS = 1;
