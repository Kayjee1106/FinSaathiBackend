package com.finsaathi.SipCalculator;

import com.finsaathi.SipCalculator.service.InvestmentSchemeService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SipCalculatorApplication {

	public static void main(String[] args) {
		SpringApplication.run(SipCalculatorApplication.class, args);
	}
	@Bean
	public CommandLineRunner initData(InvestmentSchemeService investmentSchemeService) {
		return args -> {
			// Call the service method to populate the database with initial scheme rates.
			investmentSchemeService.populateInitialSchemeData();
		};
	}
}
