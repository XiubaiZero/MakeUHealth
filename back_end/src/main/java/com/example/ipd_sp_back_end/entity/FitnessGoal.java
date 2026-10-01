package com.example.ipd_sp_back_end.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("fitness_goal")
@Data
public class FitnessGoal {
    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("user_id")
    private Integer userId;

    @TableField("goal_type")
    private String goalType;

    @TableField("current_value")
    private BigDecimal currentValue;

    @TableField("target_value")
    private BigDecimal targetValue;

    @TableField("target_date")
    private LocalDate targetDate;

    @TableField("weekly_change")
    private BigDecimal weeklyChange;

    @TableField("total_weeks")
    private Integer totalWeeks;

    @TableField("status")
    private String status;

    @TableField( "created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("completed_at")
    private LocalDateTime completedAt;
}
