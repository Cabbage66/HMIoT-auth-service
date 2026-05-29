package com.hmiot.hmiot_backend.module.policy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmiot.hmiot_backend.module.policy.entity.AuthMethod;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthMethodMapper extends BaseMapper<AuthMethod> {
}
