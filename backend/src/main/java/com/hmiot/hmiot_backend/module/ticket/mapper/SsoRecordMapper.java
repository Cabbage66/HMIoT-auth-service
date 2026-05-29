package com.hmiot.hmiot_backend.module.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmiot.hmiot_backend.module.ticket.entity.SsoRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SsoRecordMapper extends BaseMapper<SsoRecord> {
}
