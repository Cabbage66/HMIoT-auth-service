package com.hmiot.hmiot_backend.module.biometric.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmiot.hmiot_backend.module.biometric.entity.BiometricTemplate;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BiometricTemplateMapper extends BaseMapper<BiometricTemplate> {
}
