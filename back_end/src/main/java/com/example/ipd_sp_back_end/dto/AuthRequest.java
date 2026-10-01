package com.example.ipd_sp_back_end.dto;

import lombok.Data;

@Data
public class AuthRequest {
    private String account;
    private String password;
}

