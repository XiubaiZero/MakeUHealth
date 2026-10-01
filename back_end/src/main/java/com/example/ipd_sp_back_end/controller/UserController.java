package com.example.ipd_sp_back_end.controller;

import com.example.ipd_sp_back_end.entity.User;
import com.example.ipd_sp_back_end.security.CurrentAccount;
import com.example.ipd_sp_back_end.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final CurrentAccount currentAccount;

    public UserController(UserService userService, CurrentAccount currentAccount) {
        this.userService = userService;
        this.currentAccount = currentAccount;
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        Integer accountId = currentAccount.requireAccountId();
        User savedUser = userService.upsertCurrentUser(accountId, user);
        return ResponseEntity.ok(savedUser);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Integer id) {
        Integer accountId = currentAccount.requireAccountId();
        User user = userService.getCurrentUserOrThrow(accountId);
        return ResponseEntity.ok(user);
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        Integer accountId = currentAccount.requireAccountId();
        List<User> users = userService.getCurrentUserAsList(accountId);
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Integer id, @RequestBody User userDetails) {
        Integer accountId = currentAccount.requireAccountId();
        User updatedUser = userService.upsertCurrentUser(accountId, userDetails);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        Integer accountId = currentAccount.requireAccountId();
        userService.deleteCurrentUser(accountId);
        return ResponseEntity.noContent().build();
    }
}
