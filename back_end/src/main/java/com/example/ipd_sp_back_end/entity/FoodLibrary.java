package com.example.ipd_sp_back_end.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("food_library")
public class FoodLibrary {
    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("food_name")
    private String foodName;

    private Integer calories;

    private String nutrients;

    // 新增：每100g详细营养素
    private Double protein;
    private Double carbs;
    private Double fat;
    private Double fiber;
    private Double sodium;
    private Double sugar;
}