package com.example.ipd_sp_back_end.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("health_record")
@Data
public class HealthRecord {
    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("user_id")
    private Integer userId;

    @TableField("systolic")
    private Integer systolic;

    @TableField("diastolic")
    private Integer diastolic;

    @TableField("fbg")
    private Double fbg;

    @TableField("heart_rate")
    private Integer heartRate;

    @TableField("oxyhemoglobin")
    private Double oxyhemoglobin;

    @TableField("age_snapshot")
    private Integer ageSnapshot;

    @TableField("gender_snapshot")
    private String genderSnapshot;

    @TableField("height_snapshot")
    private Double heightSnapshot;

    @TableField("weight_snapshot")
    private Double weightSnapshot;

    @TableField("recorded_at")
    private LocalDateTime recordedAt;
}
