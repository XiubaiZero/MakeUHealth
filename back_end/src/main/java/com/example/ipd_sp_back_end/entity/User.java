package com.example.ipd_sp_back_end.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("user")
@Data
public class User {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer age;

    private String gender;

    @TableField("height")
    private Double height;  // cm

    @TableField("weight")
    private Double weight;  // kg

    @TableField(value = "created_at")
    private LocalDateTime createdAt;

    @TableField(value = "updated_at")
    private LocalDateTime updatedAt;
}
