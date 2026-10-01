package com.example.ipd_sp_back_end.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("auth_user")
@Data
public class AuthUser {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String account;

    @TableField("account_type")
    private String accountType;

    @TableField("password_hash")
    private String passwordHash;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

