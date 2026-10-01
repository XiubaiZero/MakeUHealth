USE `IPD`;

CREATE TABLE IF NOT EXISTS `food_library` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `food_name` VARCHAR(100) NOT NULL,
  `calories` INT NOT NULL,
  `nutrients` VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_food_library_food_name` (`food_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `food_intake` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `food_name` VARCHAR(100) NOT NULL,
  `amount` DOUBLE NOT NULL,
  `unit` VARCHAR(50) DEFAULT NULL,
  `calories` INT DEFAULT NULL,
  `nutrients` VARCHAR(255) DEFAULT NULL,
  `intake_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_food_intake_user_id` (`user_id`),
  KEY `idx_food_intake_time` (`intake_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `food_library` (`food_name`, `calories`, `nutrients`)
SELECT 'Rice', 116, 'Carbohydrate'
WHERE NOT EXISTS (
  SELECT 1 FROM `food_library` WHERE `food_name` = 'Rice'
);

INSERT INTO `food_library` (`food_name`, `calories`, `nutrients`)
SELECT 'Apple', 52, 'Vitamin C, Fiber'
WHERE NOT EXISTS (
  SELECT 1 FROM `food_library` WHERE `food_name` = 'Apple'
);

INSERT INTO `food_library` (`food_name`, `calories`, `nutrients`)
SELECT 'Egg', 155, 'Protein'
WHERE NOT EXISTS (
  SELECT 1 FROM `food_library` WHERE `food_name` = 'Egg'
);

INSERT INTO `food_library` (`food_name`, `calories`, `nutrients`)
SELECT 'Chicken Breast', 165, 'Protein'
WHERE NOT EXISTS (
  SELECT 1 FROM `food_library` WHERE `food_name` = 'Chicken Breast'
);

INSERT INTO `food_library` (`food_name`, `calories`, `nutrients`)
SELECT 'Banana', 96, 'Potassium, Vitamin B6'
WHERE NOT EXISTS (
  SELECT 1 FROM `food_library` WHERE `food_name` = 'Banana'
);

INSERT INTO `food_library` (`food_name`, `calories`, `nutrients`)
SELECT 'Milk', 42, 'Calcium, Protein'
WHERE NOT EXISTS (
  SELECT 1 FROM `food_library` WHERE `food_name` = 'Milk'
);
