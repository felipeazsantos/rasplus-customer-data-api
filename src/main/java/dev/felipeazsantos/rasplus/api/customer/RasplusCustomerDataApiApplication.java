package dev.felipeazsantos.rasplus.api.customer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class RasplusCustomerDataApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(RasplusCustomerDataApiApplication.class, args);
	}

}
