package com.hmiot.hmiot_backend.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI hmiotOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("HMIoT Auth Service API")
                        .version("v1")
                        .description("多网融合人机物电子认证服务接口"));
    }
}
