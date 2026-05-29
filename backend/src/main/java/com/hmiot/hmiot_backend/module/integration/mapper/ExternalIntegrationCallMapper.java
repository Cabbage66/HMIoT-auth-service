package com.hmiot.hmiot_backend.module.integration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmiot.hmiot_backend.module.integration.entity.ExternalIntegrationCall;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExternalIntegrationCallMapper extends BaseMapper<ExternalIntegrationCall> {
}
