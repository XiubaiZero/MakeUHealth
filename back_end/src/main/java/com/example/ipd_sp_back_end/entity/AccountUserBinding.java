package com.example.ipd_sp_back_end.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("account_user_binding")
@Data
public class AccountUserBinding {
    @TableId("account_id")
    private Integer accountId;

    @TableField("user_id")
    private Integer userId;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
