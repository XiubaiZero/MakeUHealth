package com.example.ipd_sp_back_end.controller;

import com.example.ipd_sp_back_end.dto.NutritionStats;
import com.example.ipd_sp_back_end.entity.FoodLibrary;
import com.example.ipd_sp_back_end.security.CurrentAccount;
import com.example.ipd_sp_back_end.service.FoodIntakeService;
import com.example.ipd_sp_back_end.service.FoodLibraryService;
import com.example.ipd_sp_back_end.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class FoodController {

    private final FoodLibraryService foodLibraryService;
    private final FoodIntakeService foodIntakeService;
    private final UserService userService;
    private final CurrentAccount currentAccount;

    public FoodController(
            FoodLibraryService foodLibraryService,
            FoodIntakeService foodIntakeService,
            UserService userService,
            CurrentAccount currentAccount) {
        this.foodLibraryService = foodLibraryService;
        this.foodIntakeService = foodIntakeService;
        this.userService = userService;
        this.currentAccount = currentAccount;
    }

    @GetMapping("/food-library")
    public List<FoodLibrary> getFoodLibrary() {
        return foodLibraryService.getAllFoods();
    }

    @GetMapping("/food-library/search")
    public ResponseEntity<FoodLibrary> searchFood(@RequestParam String name) {
        FoodLibrary food = foodLibraryService.getByFoodName(name);
        return food == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(food);
    }

    @GetMapping("/food-library/by-name")
    public ResponseEntity<FoodLibrary> getFoodByName(@RequestParam String foodName) {
        FoodLibrary food = foodLibraryService.getByFoodName(foodName);
        return food == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(food);
    }

    @GetMapping("/food-library/search/keyword")
    public ResponseEntity<List<FoodLibrary>> searchFoodByKeyword(@RequestParam String keyword) {
        return ResponseEntity.ok(foodLibraryService.searchByKeyword(keyword));
    }

    @GetMapping("/food-intake/stats")
    public List<NutritionStats> getStats(@RequestParam(required = false) Integer userId, @RequestParam String type) {
        Integer accountId = currentAccount.requireAccountId();
        Integer ownedUserId = userService.requireCurrentUserId(accountId);
        return foodIntakeService.getStats(ownedUserId, type);
    }
}
