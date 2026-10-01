package com.example.ipd_sp_back_end.controller;

import com.example.ipd_sp_back_end.entity.FitnessGoal;
import com.example.ipd_sp_back_end.entity.WeeklyProgress;
import com.example.ipd_sp_back_end.security.CurrentAccount;
import com.example.ipd_sp_back_end.service.FitnessGoalService;
import com.example.ipd_sp_back_end.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fitness-goals")
public class FitnessGoalController {

    private final FitnessGoalService fitnessGoalService;
    private final UserService userService;
    private final CurrentAccount currentAccount;

    public FitnessGoalController(
            FitnessGoalService fitnessGoalService,
            UserService userService,
            CurrentAccount currentAccount) {
        this.fitnessGoalService = fitnessGoalService;
        this.userService = userService;
        this.currentAccount = currentAccount;
    }

    @GetMapping("/active")
    public ResponseEntity<?> getActiveGoal() {
        try {
            Integer userId = userService.requireCurrentUserId(currentAccount.requireAccountId());
            return fitnessGoalService.getActiveGoal(userId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.ok().body((FitnessGoal) null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/active/{goalType}")
    public ResponseEntity<?> getActiveGoalByType(@PathVariable String goalType) {
        try {
            Integer userId = userService.requireCurrentUserId(currentAccount.requireAccountId());
            return fitnessGoalService.getActiveGoalByType(userId, goalType)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.ok().body((FitnessGoal) null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllGoals() {
        try {
            Integer userId = userService.requireCurrentUserId(currentAccount.requireAccountId());
            List<FitnessGoal> goals = fitnessGoalService.getAllGoals(userId);
            return ResponseEntity.ok(goals);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createGoal(@RequestBody Map<String, Object> request) {
        try {
            String goalType = (String) request.get("goalType");
            BigDecimal currentValue = new BigDecimal(request.get("currentValue").toString());
            BigDecimal targetValue = new BigDecimal(request.get("targetValue").toString());
            LocalDate targetDate = LocalDate.parse((String) request.get("targetDate"));

            Integer userId = userService.requireCurrentUserId(currentAccount.requireAccountId());
            FitnessGoal goal = fitnessGoalService.createGoal(userId, goalType, currentValue, targetValue, targetDate);
            return ResponseEntity.ok(goal);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/goal/{goalId}/progress")
    public ResponseEntity<?> recordProgress(
            @PathVariable Integer goalId,
            @RequestBody Map<String, Object> request) {
        try {
            BigDecimal currentValue = new BigDecimal(request.get("currentValue").toString());
            Integer userId = userService.requireCurrentUserId(currentAccount.requireAccountId());
            WeeklyProgress progress = fitnessGoalService.recordProgress(userId, goalId, currentValue);
            return ResponseEntity.ok(progress);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/goal/{goalId}/progress")
    public ResponseEntity<?> updateThisWeekProgress(
            @PathVariable Integer goalId,
            @RequestBody Map<String, Object> request) {
        try {
            BigDecimal currentValue = new BigDecimal(request.get("currentValue").toString());
            Integer userId = userService.requireCurrentUserId(currentAccount.requireAccountId());
            WeeklyProgress progress = fitnessGoalService.updateThisWeekProgress(userId, goalId, currentValue);
            return ResponseEntity.ok(progress);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/goal/{goalId}/progress")
    public ResponseEntity<?> getProgressByGoal(@PathVariable Integer goalId) {
        try {
            Integer userId = userService.requireCurrentUserId(currentAccount.requireAccountId());
            List<WeeklyProgress> progress = fitnessGoalService.getProgressByGoal(userId, goalId);
            return ResponseEntity.ok(progress);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/progress")
    public ResponseEntity<?> getAllProgress() {
        try {
            Integer userId = userService.requireCurrentUserId(currentAccount.requireAccountId());
            List<WeeklyProgress> progress = fitnessGoalService.getAllProgress(userId);
            return ResponseEntity.ok(progress);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/progress/{goalType}")
    public ResponseEntity<?> getProgressByGoalType(@PathVariable String goalType) {
        try {
            Integer userId = userService.requireCurrentUserId(currentAccount.requireAccountId());
            List<WeeklyProgress> progress = fitnessGoalService.getProgressByGoalType(userId, goalType);
            return ResponseEntity.ok(progress);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardData() {
        try {
            Integer userId = userService.requireCurrentUserId(currentAccount.requireAccountId());
            FitnessGoalService.DashboardData dashboard = fitnessGoalService.getDashboardData(userId);
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/dashboard/{goalType}")
    public ResponseEntity<?> getDashboardDataByType(@PathVariable String goalType) {
        try {
            Integer userId = userService.requireCurrentUserId(currentAccount.requireAccountId());
            FitnessGoalService.DashboardData dashboard = fitnessGoalService.getDashboardDataByType(userId, goalType);
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/calculate")
    public ResponseEntity<?> calculateGoal(@RequestBody Map<String, Object> request) {
        try {
            BigDecimal currentValue = new BigDecimal(request.get("currentValue").toString());
            BigDecimal targetValue = new BigDecimal(request.get("targetValue").toString());
            LocalDate targetDate = LocalDate.parse((String) request.get("targetDate"));

            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), targetDate);
            int totalWeeks = (int) Math.ceil(daysBetween / 7.0);

            BigDecimal totalChange = targetValue.subtract(currentValue);
            BigDecimal weeklyChange = totalChange.divide(BigDecimal.valueOf(totalWeeks), 3, java.math.RoundingMode.HALF_UP);

            Map<String, Object> result = new HashMap<>();
            result.put("totalWeeks", totalWeeks);
            result.put("weeklyChange", weeklyChange);
            result.put("totalChange", totalChange);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}
