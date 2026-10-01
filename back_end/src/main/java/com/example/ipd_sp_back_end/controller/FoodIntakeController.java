package com.example.ipd_sp_back_end.controller;

import com.example.ipd_sp_back_end.dto.NutritionStats;
import com.example.ipd_sp_back_end.entity.FoodIntake;
import com.example.ipd_sp_back_end.security.CurrentAccount;
import com.example.ipd_sp_back_end.service.FoodIntakeService;
import com.example.ipd_sp_back_end.service.UserService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/food-intake")
public class FoodIntakeController {

    private final FoodIntakeService foodIntakeService;
    private final UserService userService;
    private final CurrentAccount currentAccount;

    public FoodIntakeController(
            FoodIntakeService foodIntakeService,
            UserService userService,
            CurrentAccount currentAccount) {
        this.foodIntakeService = foodIntakeService;
        this.userService = userService;
        this.currentAccount = currentAccount;
    }

    @PostMapping
    public ResponseEntity<FoodIntake> addFoodIntake(@RequestBody FoodIntake foodIntake) {
        try {
            Integer accountId = currentAccount.requireAccountId();
            Integer ownedUserId = userService.requireCurrentUserId(accountId);
            return ResponseEntity.ok(foodIntakeService.createFoodIntakeForUser(ownedUserId, foodIntake));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<FoodIntake>> getFoodIntakeByUserId(@PathVariable Integer userId) {
        Integer accountId = currentAccount.requireAccountId();
        Integer ownedUserId = userService.requireCurrentUserId(accountId);
        return ResponseEntity.ok(foodIntakeService.getFoodIntakeByUserId(ownedUserId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFoodIntake(@PathVariable Integer id) {
        Integer accountId = currentAccount.requireAccountId();
        Integer ownedUserId = userService.requireCurrentUserId(accountId);
        foodIntakeService.deleteFoodIntake(ownedUserId, id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats/{userId}/{type}")
    public ResponseEntity<List<NutritionStats>> getStats(
            @PathVariable Integer userId,
            @PathVariable String type) {
        try {
            Integer accountId = currentAccount.requireAccountId();
            Integer ownedUserId = userService.requireCurrentUserId(accountId);
            return ResponseEntity.ok(foodIntakeService.getStats(ownedUserId, type));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @GetMapping("/stats/{userId}/{type}/range")
    public ResponseEntity<List<NutritionStats>> getStatsByRange(
            @PathVariable Integer userId,
            @PathVariable String type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        try {
            Integer accountId = currentAccount.requireAccountId();
            Integer ownedUserId = userService.requireCurrentUserId(accountId);
            return ResponseEntity.ok(foodIntakeService.getStatsByDateRange(ownedUserId, type, start, end));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @GetMapping("/by-meal")
    public ResponseEntity<List<Map<String, Object>>> getByMeal(
            @RequestParam String date) {
        Integer accountId = currentAccount.requireAccountId();
        Integer ownedUserId = userService.requireCurrentUserId(accountId);
        return ResponseEntity.ok(foodIntakeService.getByMeal(ownedUserId, LocalDate.parse(date)));
    }
}
