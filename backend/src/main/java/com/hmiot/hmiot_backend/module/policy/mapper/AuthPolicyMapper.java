package com.hmiot.hmiot_backend.module.policy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmiot.hmiot_backend.module.policy.entity.AuthPolicy;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthPolicyMapper extends BaseMapper<AuthPolicy> {
}
