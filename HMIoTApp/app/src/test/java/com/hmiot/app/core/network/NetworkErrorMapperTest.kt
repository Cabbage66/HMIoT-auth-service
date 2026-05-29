package com.hmiot.app.core.network

import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkErrorMapperTest {
    @Test
    fun shouldMapUnauthorized() {
        val message = NetworkErrorMapper.toMessage("401", "未认证或认证已过期", "trace-1")
        assertTrue(message.contains("认证失败或会话过期"))
        assertTrue(message.contains("trace-1"))
    }
}
