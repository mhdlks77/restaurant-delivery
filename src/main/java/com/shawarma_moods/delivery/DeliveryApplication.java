package com.shawarma_moods.delivery;

import com.shawarma_moods.delivery.config.RestaurantProperties;
import com.shawarma_moods.delivery.config.UberProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties({UberProperties.class, RestaurantProperties.class})
public class DeliveryApplication {

	private static final Logger log = LoggerFactory.getLogger(DeliveryApplication.class);
	public static void main(String[] args) {
		SpringApplication.run(DeliveryApplication.class, args);
	}

	@Bean
	CommandLineRunner logEntities(jakarta.persistence.EntityManagerFactory emf) {
		return args -> {
			emf.getMetamodel().getEntities()
					.forEach(e -> log.info("Registered JPA entity: {}", e.getName()));
		};
	}
}
