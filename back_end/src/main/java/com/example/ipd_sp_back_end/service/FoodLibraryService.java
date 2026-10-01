package com.example.ipd_sp_back_end.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.ipd_sp_back_end.entity.FoodLibrary;
import com.example.ipd_sp_back_end.mapper.FoodLibraryMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FoodLibraryService extends ServiceImpl<FoodLibraryMapper, FoodLibrary> {

    public List<FoodLibrary> getAllFoods() {
        QueryWrapper<FoodLibrary> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("food_name");
        return list(wrapper);
    }

    public FoodLibrary getByFoodName(String foodName) {
        QueryWrapper<FoodLibrary> wrapper = new QueryWrapper<>();
        wrapper.eq("food_name", foodName).last("LIMIT 1");
        return getOne(wrapper);
    }

    public List<FoodLibrary> searchByKeyword(String keyword) {
        QueryWrapper<FoodLibrary> wrapper = new QueryWrapper<>();
        wrapper.like("food_name", keyword).orderByAsc("food_name");
        return list(wrapper);
    }
}
