package com.example.ipd_sp_back_end.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.ipd_sp_back_end.entity.DailyMealTarget;
import com.example.ipd_sp_back_end.mapper.DailyMealTargetMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class DailyMealTargetService extends ServiceImpl<DailyMealTargetMapper, DailyMealTarget> {

    public DailyMealTarget getTarget(Integer userId, LocalDate date) {
        QueryWrapper<DailyMealTarget> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("target_date", date);
        return getOne(wrapper);
    }

    public DailyMealTarget saveOrUpdateTarget(DailyMealTarget target) {
        DailyMealTarget existing = getTarget(target.getUserId(), target.getTargetDate());
        if (existing != null) {
            target.setId(existing.getId());
        }
        saveOrUpdate(target);
        return target;
    }
}
