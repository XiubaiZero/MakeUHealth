package com.example.ipd_sp_back_end.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.ipd_sp_back_end.entity.HealthRecord;
import com.example.ipd_sp_back_end.entity.User;
import com.example.ipd_sp_back_end.mapper.HealthRecordMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class HealthRecordService extends ServiceImpl<HealthRecordMapper, HealthRecord> {

    private final UserService userService;

    public HealthRecordService(UserService userService) {
        this.userService = userService;
    }

    public HealthRecord createRecordForUser(Integer userId, HealthRecord record) {
        if (count() == 0) {
            baseMapper.resetAutoIncrement();
        }

        User user = userService.getById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User profile not found.");
        }

        record.setUserId(userId);
        record.setAgeSnapshot(user.getAge());
        record.setGenderSnapshot(user.getGender());
        record.setHeightSnapshot(user.getHeight());
        record.setWeightSnapshot(user.getWeight());
        save(record);
        return record;
    }

    public List<HealthRecord> getRecordsByUserId(Integer userId) {
        QueryWrapper<HealthRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("recorded_at").orderByDesc("id");
        return list(wrapper);
    }

    public HealthRecord getRecordByIdForUser(Integer userId, Integer id) {
        QueryWrapper<HealthRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("id", id).eq("user_id", userId).last("LIMIT 1");
        HealthRecord record = getOne(wrapper);
        if (record == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Health record not found.");
        }
        return record;
    }

    public void deleteRecordForUser(Integer userId, Integer id) {
        QueryWrapper<HealthRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("id", id).eq("user_id", userId);
        remove(wrapper);
        if (count() == 0) {
            baseMapper.resetAutoIncrement();
        }
    }
}
