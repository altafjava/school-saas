package com.altafjava.school.demo;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

/** Seeds the demo school at startup when {@code school.demo.seed-on-startup=true} (dev only). */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "school.demo.seed-on-startup", havingValue = "true")
@RequiredArgsConstructor
class DemoDataStartupRunner implements ApplicationRunner {

	private final DemoDataSeeder seeder;

	@Override
	public void run(ApplicationArguments args) {
		seeder.seed();
	}
}
