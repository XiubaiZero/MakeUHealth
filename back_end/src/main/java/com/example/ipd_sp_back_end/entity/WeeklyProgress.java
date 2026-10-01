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

@TableName("weekly_progress")
@Data
public class WeeklyProgress {
    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("user_id")
    private Integer userId;

    @TableField("goal_id")
    private Integer goalId;

    @TableField("week_start")
    private LocalDate weekStart;

    @TableField("week_end")
    private LocalDate weekEnd;

    @TableField("current_value")
    private BigDecimal currentValue;

    @TableField("weekly_change")
    private BigDecimal weeklyChange;

    @TableField("progress_percentage")
    private BigDecimal progressPercentage;

    @TableField("is_on_track")
    private Boolean isOnTrack;

    @TableField( "created_at")
    private LocalDateTime createdAt;
}
