package com.example.ipd_sp_back_end.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.ipd_sp_back_end.entity.AuthUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthUserMapper extends BaseMapper<AuthUser> {
}

