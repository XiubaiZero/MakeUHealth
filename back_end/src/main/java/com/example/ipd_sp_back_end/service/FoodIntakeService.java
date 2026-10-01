package com.example.ipd_sp_back_end.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.ipd_sp_back_end.dto.NutritionStats;
import com.example.ipd_sp_back_end.entity.FoodIntake;
import com.example.ipd_sp_back_end.mapper.FoodIntakeMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FoodIntakeService extends ServiceImpl<FoodIntakeMapper, FoodIntake> {

    public FoodIntake createFoodIntakeForUser(Integer userId, FoodIntake foodIntake) {
        if (foodIntake.getIntakeTime() == null) {
            foodIntake.setIntakeTime(LocalDateTime.now());
        }
        if (foodIntake.getMealType() == null || foodIntake.getMealType().isBlank()) {
            foodIntake.setMealType("snack");
        }
        foodIntake.setUserId(userId);
        save(foodIntake);
        return foodIntake;
    }

    public List<FoodIntake> getFoodIntakeByUserId(Integer userId) {
        QueryWrapper<FoodIntake> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("intake_time");
        return list(wrapper);
    }

    public List<NutritionStats> getStats(Integer userId, String type) {
        DateTimeFormatter formatter = resolveFormatter(type);

        List<FoodIntake> records = getFoodIntakeByUserId(userId);
        Map<String, List<FoodIntake>> grouped = new LinkedHashMap<>();

        for (FoodIntake record : records) {
            if (record.getIntakeTime() == null) {
                continue;
            }
            String key = record.getIntakeTime().format(formatter);
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(record);
        }

        List<NutritionStats> result = buildNutritionStats(grouped);
        Collections.reverse(result);
        return result;
    }

    public List<NutritionStats> getStatsByDateRange(Integer userId, String type, LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start time and end time are required.");
        }
        if (end.isAfter(LocalDateTime.now())) {
            end = LocalDateTime.now();
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start time cannot be after end time.");
        }

        DateTimeFormatter formatter = resolveFormatter(type);

        QueryWrapper<FoodIntake> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .ge("intake_time", start)
                .le("intake_time", end)
                .orderByAsc("intake_time");
        List<FoodIntake> records = list(wrapper);

        Map<String, List<FoodIntake>> grouped = new LinkedHashMap<>();
        for (FoodIntake record : records) {
            if (record.getIntakeTime() == null) {
                continue;
            }
            String key = record.getIntakeTime().format(formatter);
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(record);
        }
        return buildNutritionStats(grouped);
    }

    public void deleteFoodIntake(Integer userId, Integer intakeId) {
        QueryWrapper<FoodIntake> wrapper = new QueryWrapper<>();
        wrapper.eq("id", intakeId).eq("user_id", userId);
        remove(wrapper);
    }

    public List<Map<String, Object>> getByMeal(Integer userId, LocalDate date) {
        QueryWrapper<FoodIntake> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .apply("DATE(intake_time) = {0}", date)
                .orderByAsc("intake_time");
        List<FoodIntake> intakes = list(wrapper);

        Map<String, List<FoodIntake>> grouped = new LinkedHashMap<>();
        grouped.put("breakfast", new ArrayList<>());
        grouped.put("lunch", new ArrayList<>());
        grouped.put("dinner", new ArrayList<>());
        grouped.put("snack", new ArrayList<>());

        for (FoodIntake intake : intakes) {
            String mealType = intake.getMealType() == null || intake.getMealType().isBlank()
                    ? "snack"
                    : intake.getMealType();
            grouped.computeIfAbsent(mealType, ignored -> new ArrayList<>()).add(intake);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<FoodIntake>> entry : grouped.entrySet()) {
            int actualCalories = entry.getValue().stream()
                    .map(FoodIntake::getCalories)
                    .filter(java.util.Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum();

            Map<String, Object> meal = new LinkedHashMap<>();
            meal.put("mealType", entry.getKey());
            meal.put("actualCalories", actualCalories);
            meal.put("items", entry.getValue());
            result.add(meal);
        }
        return result;
    }

    private DateTimeFormatter resolveFormatter(String type) {
        return switch (type) {
            case "day" -> DateTimeFormatter.ofPattern("yyyy-MM-dd");
            case "week" -> DateTimeFormatter.ofPattern("YYYY-ww");
            case "month" -> DateTimeFormatter.ofPattern("yyyy-MM");
            default -> throw new IllegalArgumentException("type must be one of: day, week, month");
        };
    }

    private List<NutritionStats> buildNutritionStats(Map<String, List<FoodIntake>> grouped) {
        List<NutritionStats> result = new ArrayList<>();

        for (Map.Entry<String, List<FoodIntake>> entry : grouped.entrySet()) {
            int totalCalories = entry.getValue().stream()
                    .map(FoodIntake::getCalories)
                    .filter(java.util.Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum();

            Map<String, Integer> nutrientCount = new LinkedHashMap<>();
            for (FoodIntake record : entry.getValue()) {
                if (record.getNutrients() == null || record.getNutrients().isBlank()) {
                    continue;
                }
                Arrays.stream(record.getNutrients().split(","))
                        .map(String::trim)
                        .filter(part -> !part.isEmpty())
                        .forEach(nutrient -> nutrientCount.put(nutrient, nutrientCount.getOrDefault(nutrient, 0) + 1));
            }

            int totalNutrientCount = nutrientCount.values().stream().mapToInt(Integer::intValue).sum();
            StringBuilder nutrientDetail = new StringBuilder();
            for (Map.Entry<String, Integer> nutrientEntry : nutrientCount.entrySet()) {
                double ratio = totalNutrientCount == 0 ? 0 : (nutrientEntry.getValue() * 100.0) / totalNutrientCount;
                nutrientDetail.append(nutrientEntry.getKey())
                        .append("(")
                        .append(String.format("%.1f%%", ratio))
                        .append(")")
                        .append(", ");
            }
            String finalNutrient = nutrientDetail.length() > 0
                    ? nutrientDetail.substring(0, nutrientDetail.length() - 2)
                    : "No nutrients info";

            NutritionStats stats = new NutritionStats();
            stats.setPeriod(entry.getKey());
            stats.setCalories(totalCalories);
            stats.setNutrients(finalNutrient);
            result.add(stats);
        }
        return result;
    }
}
