package com.hmiot.hmiot_backend.module.biometric.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmiot.hmiot_backend.module.biometric.entity.BiometricAuthRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BiometricAuthRecordMapper extends BaseMapper<BiometricAuthRecord> {
}
