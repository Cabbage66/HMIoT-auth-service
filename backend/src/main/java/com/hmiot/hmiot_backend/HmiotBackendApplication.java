package com.hmiot.hmiot_backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.hmiot.hmiot_backend.module.**.mapper")
@SpringBootApplication
public class HmiotBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(HmiotBackendApplication.class, args);
	}

}
