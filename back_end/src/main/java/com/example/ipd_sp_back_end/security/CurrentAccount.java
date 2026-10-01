package com.example.ipd_sp_back_end.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CurrentAccount {

    public Integer requireAccountId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in first.");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Integer accountId) {
            return accountId;
        }
        if (principal instanceof String value) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException exception) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid account context.");
            }
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid account context.");
    }
}
