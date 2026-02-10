package com.example.ecommerce;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import static org.springframework.boot.SpringApplication.*;

@EnableJpaAuditing
@SpringBootApplication
public class EcommerceApiApplication {

    public static void main(String[] args) {
        run(EcommerceApiApplication.class, args);
    }
}
