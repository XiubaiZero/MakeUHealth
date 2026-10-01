package com.example.ipd_sp_back_end.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("food_intake")
public class FoodIntake {
    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("user_id")
    private Integer userId;

    @TableField("food_name")
    private String foodName;

    private Double amount;

    private String unit;

    private Integer calories;

    private String nutrients;

    @TableField("intake_time")
    private LocalDateTime intakeTime;

    @TableField("meal_type")
    private String mealType;
}
