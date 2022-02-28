package it.francesco.synthesia.job;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
public class SynthesiaJobApplication {

    public static void main(String[] args) {
        SpringApplication.run(SynthesiaJobApplication.class, args);
    }

}