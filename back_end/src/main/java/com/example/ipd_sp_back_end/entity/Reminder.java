package com.example.ipd_sp_back_end.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("reminder")
@Data
public class Reminder {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("account_id")
    private Integer accountId;

    @TableField("user_id")
    private Integer userId;

    @TableField("reminder_type")
    private String reminderType;

    @TableField("reminder_time")
    private LocalDateTime reminderTime;

    @TableField("repeat_pattern")
    private String repeatPattern;

    @TableField("note")
    private String note;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
