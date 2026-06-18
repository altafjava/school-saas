package com.altafjava.school;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// SecurityConfig is imported automatically via PlatformAutoConfiguration — no @Import needed here
@SpringBootApplication(scanBasePackages = { "com.altafjava.school", "com.altafjava.platform" })
public class SchoolTestApplication {

	public static void main(String[] args) {
		SpringApplication.run(SchoolTestApplication.class, args);
	}
}
