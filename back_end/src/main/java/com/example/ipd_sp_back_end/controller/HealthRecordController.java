package com.example.ipd_sp_back_end.controller;

import com.example.ipd_sp_back_end.entity.HealthRecord;
import com.example.ipd_sp_back_end.security.CurrentAccount;
import com.example.ipd_sp_back_end.service.HealthRecordService;
import com.example.ipd_sp_back_end.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/health-records")
public class HealthRecordController {

    private final HealthRecordService healthRecordService;
    private final UserService userService;
    private final CurrentAccount currentAccount;

    public HealthRecordController(
            HealthRecordService healthRecordService,
            UserService userService,
            CurrentAccount currentAccount) {
        this.healthRecordService = healthRecordService;
        this.userService = userService;
        this.currentAccount = currentAccount;
    }

    @PostMapping("/user/{userId}")
    public ResponseEntity<HealthRecord> createRecord(
            @PathVariable Integer userId,
            @RequestBody HealthRecord record) {
        Integer accountId = currentAccount.requireAccountId();
        Integer ownedUserId = userService.requireCurrentUserId(accountId);
        HealthRecord savedRecord = healthRecordService.createRecordForUser(ownedUserId, record);
        return ResponseEntity.ok(savedRecord);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<HealthRecord>> getRecordsByUserId(@PathVariable Integer userId) {
        Integer accountId = currentAccount.requireAccountId();
        Integer ownedUserId = userService.requireCurrentUserId(accountId);
        List<HealthRecord> records = healthRecordService.getRecordsByUserId(ownedUserId);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HealthRecord> getRecordById(@PathVariable Integer id) {
        Integer accountId = currentAccount.requireAccountId();
        Integer ownedUserId = userService.requireCurrentUserId(accountId);
        HealthRecord record = healthRecordService.getRecordByIdForUser(ownedUserId, id);
        return ResponseEntity.ok(record);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecord(@PathVariable Integer id) {
        Integer accountId = currentAccount.requireAccountId();
        Integer ownedUserId = userService.requireCurrentUserId(accountId);
        healthRecordService.deleteRecordForUser(ownedUserId, id);
        return ResponseEntity.noContent().build();
    }
}
