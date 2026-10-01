-- ========== Initialize Food Library with Detailed Nutrients (per 100g) ==========

INSERT INTO `food_library` (`food_name`, `calories`, `nutrients`, `protein`, `carbs`, `fat`, `fiber`, `sodium`, `sugar`)
SELECT 'Rice', 116, 'Carbohydrate', 2.6, 25.9, 0.3, 0.4, 5.0, 0.1
    WHERE NOT EXISTS (
  SELECT 1 FROM `food_library` WHERE `food_name` = 'Rice'
);

INSERT INTO `food_library` (`food_name`, `calories`, `nutrients`, `protein`, `carbs`, `fat`, `fiber`, `sodium`, `sugar`)
SELECT 'Apple', 52, 'Vitamin C, Fiber', 0.3, 13.8, 0.2, 2.4, 1.0, 10.4
    WHERE NOT EXISTS (
  SELECT 1 FROM `food_library` WHERE `food_name` = 'Apple'
);

INSERT INTO `food_library` (`food_name`, `calories`, `nutrients`, `protein`, `carbs`, `fat`, `fiber`, `sodium`, `sugar`)
SELECT 'Egg', 155, 'Protein', 13.0, 1.1, 11.0, 0.0, 124.0, 1.1
    WHERE NOT EXISTS (
  SELECT 1 FROM `food_library` WHERE `food_name` = 'Egg'
);

INSERT INTO `food_library` (`food_name`, `calories`, `nutrients`, `protein`, `carbs`, `fat`, `fiber`, `sodium`, `sugar`)
SELECT 'Chicken Breast', 165, 'Protein', 31.0, 0.0, 3.6, 0.0, 74.0, 0.0
    WHERE NOT EXISTS (
  SELECT 1 FROM `food_library` WHERE `food_name` = 'Chicken Breast'
);

INSERT INTO `food_library` (`food_name`, `calories`, `nutrients`, `protein`, `carbs`, `fat`, `fiber`, `sodium`, `sugar`)
SELECT 'Banana', 96, 'Potassium, Vitamin B6', 1.1, 23.0, 0.3, 2.6, 1.0, 12.2
    WHERE NOT EXISTS (
  SELECT 1 FROM `food_library` WHERE `food_name` = 'Banana'
);

INSERT INTO `food_library` (`food_name`, `calories`, `nutrients`, `protein`, `carbs`, `fat`, `fiber`, `sodium`, `sugar`)
SELECT 'Milk', 42, 'Calcium, Protein', 3.4, 4.8, 1.0, 0.0, 44.0, 5.1
    WHERE NOT EXISTS (
  SELECT 1 FROM `food_library` WHERE `food_name` = 'Milk'
);