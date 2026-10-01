package com.example.ipd_sp_back_end.dto;

import lombok.Data;

@Data
public class AuthResponse {
    private Integer accountId;
    private String account;
    private String accountType;
    private String token;
    private String message;
}

