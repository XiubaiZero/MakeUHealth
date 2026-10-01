package com.example.ipd_sp_back_end.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.ipd_sp_back_end.dto.AuthRequest;
import com.example.ipd_sp_back_end.dto.AuthResponse;
import com.example.ipd_sp_back_end.entity.AuthUser;
import com.example.ipd_sp_back_end.mapper.AuthUserMapper;
import com.example.ipd_sp_back_end.security.JwtService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AuthService extends ServiceImpl<AuthUserMapper, AuthUser> {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern LOOSE_PHONE_PATTERN = Pattern.compile("^\\+?[0-9][0-9\\s-]{6,18}$");
    private static final Pattern NORMALIZED_PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{7,15}$");
    private static final int MIN_PASSWORD_LENGTH = 6;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtService jwtService;

    public AuthService(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public AuthResponse register(AuthRequest request) {
        NormalizedAccount normalizedAccount = normalizeAndValidateAccount(request.getAccount());
        String password = requirePassword(request.getPassword());

        boolean accountExists = lambdaQuery()
                .eq(AuthUser::getAccount, normalizedAccount.normalizedValue())
                .exists();
        if (accountExists) {
            throw new IllegalArgumentException("This account is already registered. Please sign in.");
        }

        AuthUser authUser = new AuthUser();
        authUser.setAccount(normalizedAccount.normalizedValue());
        authUser.setAccountType(normalizedAccount.accountType());
        authUser.setPasswordHash(passwordEncoder.encode(password));
        save(authUser);

        AuthResponse response = new AuthResponse();
        response.setAccountId(authUser.getId());
        response.setAccount(authUser.getAccount());
        response.setAccountType(authUser.getAccountType());
        response.setMessage("Registration successful.");
        return response;
    }

    public AuthResponse login(AuthRequest request) {
        NormalizedAccount normalizedAccount = normalizeAndValidateAccount(request.getAccount());
        String password = requirePassword(request.getPassword());

        AuthUser authUser = lambdaQuery()
                .eq(AuthUser::getAccount, normalizedAccount.normalizedValue())
                .last("LIMIT 1")
                .one();

        if (authUser == null) {
            throw new IllegalArgumentException("This account does not exist. Please register first.");
        }

        if (!passwordEncoder.matches(password, authUser.getPasswordHash())) {
            throw new IllegalArgumentException("Incorrect password. Please try again.");
        }

        AuthResponse response = new AuthResponse();
        response.setAccountId(authUser.getId());
        response.setAccount(authUser.getAccount());
        response.setAccountType(authUser.getAccountType());
        response.setToken(jwtService.generateToken(authUser.getId(), authUser.getAccount()));
        response.setMessage("Login successful.");
        return response;
    }

    private String requirePassword(String password) {
        String value = password == null ? "" : password.trim();
        if (value.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must contain at least 6 characters.");
        }
        return value;
    }

    private NormalizedAccount normalizeAndValidateAccount(String account) {
        String value = account == null ? "" : account.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Please enter your email or phone number.");
        }

        if (EMAIL_PATTERN.matcher(value).matches()) {
            return new NormalizedAccount(value.toLowerCase(Locale.ROOT), "email");
        }

        if (LOOSE_PHONE_PATTERN.matcher(value).matches()) {
            String normalizedPhone = value.replaceAll("[\\s-]", "");
            if (NORMALIZED_PHONE_PATTERN.matcher(normalizedPhone).matches()) {
                return new NormalizedAccount(normalizedPhone, "phone");
            }
        }

        throw new IllegalArgumentException("Please use a valid email format or phone number format.");
    }

    private record NormalizedAccount(String normalizedValue, String accountType) {
    }
}
