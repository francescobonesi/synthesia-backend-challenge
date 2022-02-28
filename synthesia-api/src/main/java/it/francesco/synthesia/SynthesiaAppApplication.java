package it.francesco.synthesia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
public class SynthesiaAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(SynthesiaAppApplication.class, args);
	}

}
