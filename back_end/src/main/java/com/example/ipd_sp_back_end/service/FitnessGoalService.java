package com.example.ipd_sp_back_end.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.ipd_sp_back_end.entity.FitnessGoal;
import com.example.ipd_sp_back_end.entity.WeeklyProgress;
import com.example.ipd_sp_back_end.mapper.FitnessGoalMapper;
import com.example.ipd_sp_back_end.mapper.WeeklyProgressMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class FitnessGoalService extends ServiceImpl<FitnessGoalMapper, FitnessGoal> {

    @Autowired
    private WeeklyProgressMapper weeklyProgressMapper;

    // Get active goal by type
    public Optional<FitnessGoal> getActiveGoalByType(Integer userId, String goalType) {
        QueryWrapper<FitnessGoal> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("status", "active").eq("goal_type", goalType);
        return Optional.ofNullable(getOne(wrapper));
    }

    // Get latest goal by type
    public Optional<FitnessGoal> getLatestGoalByType(Integer userId, String goalType) {
        QueryWrapper<FitnessGoal> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .eq("goal_type", goalType)
               .in("status", "active", "completed")
               .orderByDesc("created_at")
               .last("LIMIT 1");
        return Optional.ofNullable(getOne(wrapper));
    }

    // Get all active goals
    public Optional<FitnessGoal> getActiveGoal(Integer userId) {
        QueryWrapper<FitnessGoal> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("status", "active").orderByDesc("created_at").last("LIMIT 1");
        return Optional.ofNullable(getOne(wrapper));
    }

    // Get all goals
    public List<FitnessGoal> getAllGoals(Integer userId) {
        QueryWrapper<FitnessGoal> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.orderByDesc("created_at");
        return list(wrapper);
    }

    // Create new fitness goal
    @Transactional
    public FitnessGoal createGoal(Integer userId, String goalType,
                                   BigDecimal currentValue, BigDecimal targetValue,
                                   LocalDate targetDate) {
        if (targetDate.isBefore(LocalDate.now().plusDays(7))) {
            throw new RuntimeException("Target date must be at least 7 days from today");
        }

        Optional<FitnessGoal> existingGoal = getActiveGoalByType(userId, goalType);
        existingGoal.ifPresent(goal -> {
            goal.setStatus("archived");
            updateById(goal);
        });

        long daysBetween = ChronoUnit.DAYS.between(LocalDate.now(), targetDate);
        int totalWeeks = (int) Math.ceil(daysBetween / 7.0);

        BigDecimal totalChange = targetValue.subtract(currentValue);
        BigDecimal weeklyChange = totalChange.divide(BigDecimal.valueOf(totalWeeks), 3, RoundingMode.HALF_UP);

        FitnessGoal goal = new FitnessGoal();
        goal.setUserId(userId);
        goal.setGoalType(goalType);
        goal.setCurrentValue(currentValue);
        goal.setTargetValue(targetValue);
        goal.setTargetDate(targetDate);
        goal.setWeeklyChange(weeklyChange);
        goal.setTotalWeeks(totalWeeks);
        goal.setStatus("active");

        save(goal);
        return goal;
    }

    // Record weekly progress
    @Transactional
    public WeeklyProgress recordProgress(Integer userId, Integer goalId, BigDecimal currentValue) {
        QueryWrapper<FitnessGoal> goalWrapper = new QueryWrapper<>();
        goalWrapper.eq("id", goalId).eq("user_id", userId).last("LIMIT 1");
        FitnessGoal goal = getOne(goalWrapper);
        if (goal == null) {
            throw new RuntimeException("Goal not found");
        }


        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        QueryWrapper<WeeklyProgress> wrapper = new QueryWrapper<>();
        wrapper.eq("goal_id", goalId).eq("user_id", userId).eq("week_start", weekStart);
        WeeklyProgress existingProgress = weeklyProgressMapper.selectOne(wrapper);

        if (existingProgress != null) {
            throw new RuntimeException("Progress for this week has already been recorded");
        }

        QueryWrapper<WeeklyProgress> lastWrapper = new QueryWrapper<>();
        lastWrapper.eq("goal_id", goalId).eq("user_id", userId).orderByDesc("week_start").last("LIMIT 1");
        WeeklyProgress lastProgress = weeklyProgressMapper.selectOne(lastWrapper);

        BigDecimal weeklyChange = BigDecimal.ZERO;
        if (lastProgress != null) {
            weeklyChange = currentValue.subtract(lastProgress.getCurrentValue());
        } else {
            weeklyChange = currentValue.subtract(goal.getCurrentValue());
        }

        BigDecimal totalTargetChange = goal.getTargetValue().subtract(goal.getCurrentValue());
        BigDecimal currentChange = currentValue.subtract(goal.getCurrentValue());

        BigDecimal progressPercentage;
        if (totalTargetChange.compareTo(BigDecimal.ZERO) == 0) {
            progressPercentage = BigDecimal.valueOf(100);
        } else {
            progressPercentage = currentChange.divide(totalTargetChange, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            if (progressPercentage.compareTo(BigDecimal.ZERO) < 0) {
                progressPercentage = BigDecimal.ZERO;
            } else if (progressPercentage.compareTo(BigDecimal.valueOf(100)) > 0) {
                progressPercentage = BigDecimal.valueOf(100);
            }
        }

        boolean isOnTrack = checkIfOnTrack(goal, weeklyChange);

        WeeklyProgress progress = new WeeklyProgress();
        progress.setUserId(userId);
        progress.setGoalId(goalId);
        progress.setWeekStart(weekStart);
        progress.setWeekEnd(weekEnd);
        progress.setCurrentValue(currentValue);
        progress.setWeeklyChange(weeklyChange);
        progress.setProgressPercentage(progressPercentage);
        progress.setIsOnTrack(isOnTrack);

        if (progressPercentage.compareTo(BigDecimal.valueOf(100)) >= 0) {
            goal.setStatus("completed");
            goal.setCompletedAt(java.time.LocalDateTime.now());
            updateById(goal);
        }

        weeklyProgressMapper.insert(progress);
        return progress;
    }

    @Transactional
    public WeeklyProgress updateThisWeekProgress(Integer userId, Integer goalId, BigDecimal currentValue) {
        QueryWrapper<FitnessGoal> goalWrapper = new QueryWrapper<>();
        goalWrapper.eq("id", goalId).eq("user_id", userId).last("LIMIT 1");
        FitnessGoal goal = getOne(goalWrapper);
        if (goal == null) {
            throw new RuntimeException("Goal not found");
        }

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);

        QueryWrapper<WeeklyProgress> wrapper = new QueryWrapper<>();
        wrapper.eq("goal_id", goalId).eq("user_id", userId).eq("week_start", weekStart);
        WeeklyProgress existingProgress = weeklyProgressMapper.selectOne(wrapper);

        if (existingProgress == null) {
            throw new RuntimeException("No progress found for this week. Please record progress first.");
        }

        QueryWrapper<WeeklyProgress> lastWrapper = new QueryWrapper<>();
        lastWrapper.eq("goal_id", goalId)
                   .eq("user_id", userId)
                   .lt("week_start", weekStart)
                   .orderByDesc("week_start")
                   .last("LIMIT 1");
        WeeklyProgress lastProgress = weeklyProgressMapper.selectOne(lastWrapper);

        BigDecimal weeklyChange;
        if (lastProgress != null) {
            weeklyChange = currentValue.subtract(lastProgress.getCurrentValue());
        } else {
            weeklyChange = currentValue.subtract(goal.getCurrentValue());
        }

        BigDecimal totalTargetChange = goal.getTargetValue().subtract(goal.getCurrentValue());
        BigDecimal currentChange = currentValue.subtract(goal.getCurrentValue());

        BigDecimal progressPercentage;
        if (totalTargetChange.compareTo(BigDecimal.ZERO) == 0) {
            progressPercentage = BigDecimal.valueOf(100);
        } else {
            progressPercentage = currentChange.divide(totalTargetChange, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            if (progressPercentage.compareTo(BigDecimal.ZERO) < 0) {
                progressPercentage = BigDecimal.ZERO;
            } else if (progressPercentage.compareTo(BigDecimal.valueOf(100)) > 0) {
                progressPercentage = BigDecimal.valueOf(100);
            }
        }

        boolean isOnTrack = checkIfOnTrack(goal, weeklyChange);

        existingProgress.setCurrentValue(currentValue);
        existingProgress.setWeeklyChange(weeklyChange);
        existingProgress.setProgressPercentage(progressPercentage);
        existingProgress.setIsOnTrack(isOnTrack);

        weeklyProgressMapper.updateById(existingProgress);

        if (progressPercentage.compareTo(BigDecimal.valueOf(100)) >= 0) {
            goal.setStatus("completed");
            goal.setCompletedAt(java.time.LocalDateTime.now());
            updateById(goal);
        }

        return existingProgress;
    }

    private boolean checkIfOnTrack(FitnessGoal goal, BigDecimal weeklyChange) {
        BigDecimal targetWeeklyChange = goal.getWeeklyChange();
        String goalType = goal.getGoalType();

        if ("weight_loss".equals(goalType) || "fat_loss".equals(goalType)) {
            return weeklyChange.compareTo(targetWeeklyChange) <= 0;
        } else if ("muscle_gain".equals(goalType)) {
            return weeklyChange.compareTo(targetWeeklyChange) >= 0;
        } else {
            return false;
        }
    }

    public List<WeeklyProgress> getProgressByGoal(Integer userId, Integer goalId) {
        QueryWrapper<WeeklyProgress> wrapper = new QueryWrapper<>();
        wrapper.eq("goal_id", goalId).eq("user_id", userId).orderByDesc("week_start");
        return weeklyProgressMapper.selectList(wrapper);
    }

    public List<WeeklyProgress> getAllProgress(Integer userId) {
        QueryWrapper<WeeklyProgress> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("week_start");
        return weeklyProgressMapper.selectList(wrapper);
    }

    public List<WeeklyProgress> getProgressByGoalType(Integer userId, String goalType) {
        Optional<FitnessGoal> goal = getLatestGoalByType(userId, goalType);
        if (goal.isPresent()) {
            return getProgressByGoal(userId, goal.get().getId());
        }
        return List.of();
    }

    public DashboardData getDashboardData(Integer userId) {
        DashboardData dashboard = new DashboardData();

        Optional<FitnessGoal> activeGoal = getActiveGoal(userId);
        if (activeGoal.isPresent()) {
            FitnessGoal goal = activeGoal.get();
            dashboard.setActiveGoal(goal);

            QueryWrapper<WeeklyProgress> latestWrapper = new QueryWrapper<>();
            latestWrapper.eq("goal_id", goal.getId()).eq("user_id", userId).orderByDesc("week_start").last("LIMIT 1");
            WeeklyProgress latestProgress = weeklyProgressMapper.selectOne(latestWrapper);
            dashboard.setLatestProgress(latestProgress);

            long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), goal.getTargetDate());
            int remainingWeeks = (int) Math.max(0, Math.ceil(daysRemaining / 7.0));
            dashboard.setRemainingWeeks(remainingWeeks);

            LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
            QueryWrapper<WeeklyProgress> thisWeekWrapper = new QueryWrapper<>();
            thisWeekWrapper.eq("goal_id", goal.getId()).eq("user_id", userId).eq("week_start", weekStart);
            WeeklyProgress thisWeekProgress = weeklyProgressMapper.selectOne(thisWeekWrapper);
            dashboard.setThisWeekProgress(thisWeekProgress);
        }

        return dashboard;
    }

    public DashboardData getDashboardDataByType(Integer userId, String goalType) {
        DashboardData dashboard = new DashboardData();

        Optional<FitnessGoal> activeGoal = getLatestGoalByType(userId, goalType);
        if (activeGoal.isPresent()) {
            FitnessGoal goal = activeGoal.get();
            dashboard.setActiveGoal(goal);

            QueryWrapper<WeeklyProgress> latestWrapper = new QueryWrapper<>();
            latestWrapper.eq("goal_id", goal.getId()).eq("user_id", userId).orderByDesc("week_start").last("LIMIT 1");
            WeeklyProgress latestProgress = weeklyProgressMapper.selectOne(latestWrapper);
            dashboard.setLatestProgress(latestProgress);

            long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), goal.getTargetDate());
            int remainingWeeks = (int) Math.max(0, Math.ceil(daysRemaining / 7.0));
            dashboard.setRemainingWeeks(remainingWeeks);

            LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
            QueryWrapper<WeeklyProgress> thisWeekWrapper = new QueryWrapper<>();
            thisWeekWrapper.eq("goal_id", goal.getId()).eq("user_id", userId).eq("week_start", weekStart);
            WeeklyProgress thisWeekProgress = weeklyProgressMapper.selectOne(thisWeekWrapper);
            dashboard.setThisWeekProgress(thisWeekProgress);
        }

        return dashboard;
    }

    public static class DashboardData {
        private FitnessGoal activeGoal;
        private WeeklyProgress latestProgress;
        private WeeklyProgress thisWeekProgress;
        private int remainingWeeks;

        public FitnessGoal getActiveGoal() { return activeGoal; }
        public void setActiveGoal(FitnessGoal activeGoal) { this.activeGoal = activeGoal; }

        public WeeklyProgress getLatestProgress() { return latestProgress; }
        public void setLatestProgress(WeeklyProgress latestProgress) { this.latestProgress = latestProgress; }

        public WeeklyProgress getThisWeekProgress() { return thisWeekProgress; }
        public void setThisWeekProgress(WeeklyProgress thisWeekProgress) { this.thisWeekProgress = thisWeekProgress; }

        public int getRemainingWeeks() { return remainingWeeks; }
        public void setRemainingWeeks(int remainingWeeks) { this.remainingWeeks = remainingWeeks; }
    }
}
