package com.example.ipd_sp_back_end.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NutritionStats {
    private String period;
    private Integer calories;
    private String nutrients;

    // 新增：详细营养素（用于生理影响面板）
    private Double protein;
    private Double carbs;
    private Double fat;
    private Double fiber;
    private Double sodium;
    private Double sugar;
}