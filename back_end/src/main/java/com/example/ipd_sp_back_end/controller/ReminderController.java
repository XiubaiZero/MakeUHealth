package com.example.ipd_sp_back_end.controller;

import com.example.ipd_sp_back_end.entity.Reminder;
import com.example.ipd_sp_back_end.security.CurrentAccount;
import com.example.ipd_sp_back_end.service.ReminderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reminders")
public class ReminderController {

    private static final Logger log = LoggerFactory.getLogger(ReminderController.class);

    private final ReminderService reminderService;
    private final CurrentAccount currentAccount;

    public ReminderController(ReminderService reminderService, CurrentAccount currentAccount) {
        this.reminderService = reminderService;
        this.currentAccount = currentAccount;
    }

    @PostMapping
    public ResponseEntity<Reminder> createReminder(@RequestBody Reminder reminder) {
        Integer accountId = currentAccount.requireAccountId();
        log.info("Create reminder - User: {}, Type: {}, Time: {}", accountId, reminder.getReminderType(), reminder.getReminderTime());
        return ResponseEntity.ok(reminderService.saveReminder(accountId, reminder));
    }

    @GetMapping
    public ResponseEntity<List<Reminder>> getAccountReminders() {
        Integer accountId = currentAccount.requireAccountId();
        log.info("Get reminders for account: {}", accountId);
        return ResponseEntity.ok(reminderService.getAccountReminders(accountId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Reminder>> getUserReminders(
            @PathVariable Integer userId) {
        Integer accountId = currentAccount.requireAccountId();
        log.info("Get reminders - User: {}, TargetUserId: {}", accountId, userId);
        return ResponseEntity.ok(reminderService.getUserReminders(accountId, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReminder(@PathVariable Long id) {
        Integer accountId = currentAccount.requireAccountId();
        log.info("Delete reminder - User: {}, ReminderId: {}", accountId, id);
        reminderService.deleteReminder(accountId, id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Reminder> updateReminder(
            @PathVariable Long id,
            @RequestBody Reminder reminder) {
        Integer accountId = currentAccount.requireAccountId();
        log.info("Update reminder - User: {}, ReminderId: {}, Type: {}", accountId, id, reminder.getReminderType());
        return ResponseEntity.ok(reminderService.updateReminder(accountId, id, reminder));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAccountReminders() {
        Integer accountId = currentAccount.requireAccountId();
        log.info("Delete all reminders for account: {}", accountId);
        reminderService.deleteAccountReminders(accountId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> deleteUserReminders(
            @PathVariable Integer userId) {
        Integer accountId = currentAccount.requireAccountId();
        log.info("Delete all reminders - User: {}, TargetUserId: {}", accountId, userId);
        reminderService.deleteUserReminders(accountId, userId);
        return ResponseEntity.ok().build();
    }
}
