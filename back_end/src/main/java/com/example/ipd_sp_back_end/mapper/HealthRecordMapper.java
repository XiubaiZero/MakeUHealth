package com.example.ipd_sp_back_end.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.ipd_sp_back_end.entity.HealthRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface HealthRecordMapper extends BaseMapper<HealthRecord> {
    @Update("ALTER TABLE `health_record` AUTO_INCREMENT = 1")
    void resetAutoIncrement();
}
