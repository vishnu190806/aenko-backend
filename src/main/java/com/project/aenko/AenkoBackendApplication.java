package com.project.aenko;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class AenkoBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(AenkoBackendApplication.class, args);
	}

}
