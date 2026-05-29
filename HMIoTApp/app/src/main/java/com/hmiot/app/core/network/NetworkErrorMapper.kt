package com.hmiot.app.core.network

object NetworkErrorMapper {
    fun toMessage(code: String, message: String, traceId: String?): String {
        if (code == "NETWORK_ERROR" && (message.contains("10.0.2.2") || message.contains("Failed to connect"))) {
            return "请求失败: 无法连接后端。请确认后端已启动在8080；模拟器走10.0.2.2，真机(adb reverse)走127.0.0.1, traceId=${traceId ?: "-"}"
        }
        val prefix = when (code) {
            "401" -> "认证失败或会话过期"
            "403" -> "访问被拒绝"
            "400" -> "请求参数错误"
            "0" -> "请求成功"
            else -> "请求失败"
        }
        return "$prefix: $message, traceId=${traceId ?: "-"}"
    }
}
