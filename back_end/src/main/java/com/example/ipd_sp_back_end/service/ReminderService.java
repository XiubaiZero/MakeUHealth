package com.example.ipd_sp_back_end.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.ipd_sp_back_end.entity.Reminder;
import com.example.ipd_sp_back_end.mapper.ReminderMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ReminderService extends ServiceImpl<ReminderMapper, Reminder> {

    public Reminder saveReminder(Integer accountId, Reminder reminder) {
        reminder.setId(null);
        reminder.setAccountId(accountId);
        reminder.setUserId(accountId);
        if (reminder.getEnabled() == null) {
            reminder.setEnabled(true);
        }
        save(reminder);
        return reminder;
    }

    public List<Reminder> getAccountReminders(Integer accountId) {
        QueryWrapper<Reminder> wrapper = new QueryWrapper<>();
        wrapper.eq("account_id", accountId)
                .orderByAsc("reminder_time")
                .orderByDesc("created_at");
        return list(wrapper);
    }

    public List<Reminder> getUserReminders(Integer accountId, Integer userId) {
        return getAccountReminders(accountId);
    }

    public Reminder updateReminder(Integer accountId, Long id, Reminder reminder) {
        Reminder existing = requireOwnedReminder(accountId, id);
        existing.setUserId(accountId);
        existing.setReminderType(reminder.getReminderType());
        existing.setReminderTime(reminder.getReminderTime());
        existing.setRepeatPattern(reminder.getRepeatPattern());
        existing.setNote(reminder.getNote());
        existing.setEnabled(reminder.getEnabled() == null ? existing.getEnabled() : reminder.getEnabled());
        updateById(existing);
        return existing;
    }

    public void deleteReminder(Integer accountId, Long id) {
        requireOwnedReminder(accountId, id);
        removeById(id);
    }

    public void deleteUserReminders(Integer accountId, Integer userId) {
        deleteAccountReminders(accountId);
    }

    public void deleteAccountReminders(Integer accountId) {
        QueryWrapper<Reminder> wrapper = new QueryWrapper<>();
        wrapper.eq("account_id", accountId);
        remove(wrapper);
    }

    private Reminder requireOwnedReminder(Integer accountId, Long id) {
        Reminder reminder = lambdaQuery()
                .eq(Reminder::getId, id)
                .eq(Reminder::getAccountId, accountId)
                .last("LIMIT 1")
                .one();
        if (reminder == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reminder not found.");
        }
        return reminder;
    }
}
