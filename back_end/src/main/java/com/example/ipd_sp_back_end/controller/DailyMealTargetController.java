package com.example.ipd_sp_back_end.controller;

import com.example.ipd_sp_back_end.entity.DailyMealTarget;
import com.example.ipd_sp_back_end.security.CurrentAccount;
import com.example.ipd_sp_back_end.service.DailyMealTargetService;
import com.example.ipd_sp_back_end.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/meal-targets")
public class DailyMealTargetController {

    private final DailyMealTargetService dailyMealTargetService;
    private final UserService userService;
    private final CurrentAccount currentAccount;

    public DailyMealTargetController(
            DailyMealTargetService dailyMealTargetService,
            UserService userService,
            CurrentAccount currentAccount) {
        this.dailyMealTargetService = dailyMealTargetService;
        this.userService = userService;
        this.currentAccount = currentAccount;
    }

    @GetMapping
    public ResponseEntity<DailyMealTarget> getTarget(
            @RequestParam String date) {
        Integer accountId = currentAccount.requireAccountId();
        Integer ownedUserId = userService.getCurrentUser(accountId)
                .map(com.example.ipd_sp_back_end.entity.User::getId)
                .orElse(null);
        if (ownedUserId == null) {
            return ResponseEntity.ok(null);
        }
        DailyMealTarget target = dailyMealTargetService.getTarget(ownedUserId, LocalDate.parse(date));
        return ResponseEntity.ok(target);
    }

    @PostMapping
    public ResponseEntity<DailyMealTarget> saveTarget(@RequestBody DailyMealTarget target) {
        Integer accountId = currentAccount.requireAccountId();
        Integer ownedUserId = userService.requireCurrentUserId(accountId);
        target.setUserId(ownedUserId);
        return ResponseEntity.ok(dailyMealTargetService.saveOrUpdateTarget(target));
    }
}
