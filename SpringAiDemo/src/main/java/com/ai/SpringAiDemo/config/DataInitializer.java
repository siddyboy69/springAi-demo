package com.ai.SpringAiDemo.config;

import com.ai.SpringAiDemo.domain.Allergy;
import com.ai.SpringAiDemo.domain.User;
import com.ai.SpringAiDemo.persistence.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    public DataInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    @Override
    public void run(String... args) throws Exception {
        userRepository.deleteAll();

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

        userRepository.save(user1);
        userRepository.save(user2);
    }
}
