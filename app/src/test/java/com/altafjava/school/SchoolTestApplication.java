package com.altafjava.school;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import com.altafjava.platform.infrastructure.config.SecurityConfig;

@SpringBootApplication(scanBasePackages = {"com.altafjava.school", "com.altafjava.platform"})
@Import(SecurityConfig.class)
public class SchoolTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchoolTestApplication.class, args);
    }
}
