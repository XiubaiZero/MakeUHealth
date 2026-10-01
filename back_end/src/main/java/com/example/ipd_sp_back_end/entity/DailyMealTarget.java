package com.example.ipd_sp_back_end.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("daily_meal_target")
public class DailyMealTarget {
    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("user_id")
    private Integer userId;

    @TableField("target_date")
    private LocalDate targetDate;

    @TableField("breakfast_target")
    private Integer breakfastTarget;

    @TableField("lunch_target")
    private Integer lunchTarget;

    @TableField("dinner_target")
    private Integer dinnerTarget;

    @TableField("snack_target")
    private Integer snackTarget;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
