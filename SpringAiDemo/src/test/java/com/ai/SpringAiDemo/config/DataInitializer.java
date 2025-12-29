package com.ai.SpringAiDemo.config;

import com.ai.SpringAiDemo.domain.Allergy;
import com.ai.SpringAiDemo.domain.User;
import com.ai.SpringAiDemo.persistence.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository) {
        return args -> {
            // Create test users with allergies
            User user1 = User.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .allergies(List.of(
                            new Allergy("peanuts"),
                            new Allergy("shellfish"),
                            new Allergy("dairy")
                    ))
                    .build();

            User user2 = User.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .allergies(List.of(
                            new Allergy("gluten"),
                            new Allergy("soy")
                    ))
                    .build();

            userRepository.saveAll(List.of(user1, user2));

            System.out.println("✓ Test users created with IDs: " +
                    user1.getId() + ", " + user2.getId());
        };
    }
}
