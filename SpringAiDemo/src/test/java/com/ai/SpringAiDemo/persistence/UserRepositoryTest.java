package com.ai.SpringAiDemo.persistence;

import com.ai.SpringAiDemo.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static com.ai.SpringAiDemo.TestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
@DataJpaTest
class UserRepositoryTest {
    private @Autowired UserRepository userRepository;

    @Test
    void ensureSaveAndReadWorks(){
        User user = user();
        var saved = userRepository.saveAndFlush(user);
        assertThat(saved).isSameAs(user);
        assertThat(saved.getId()).isNotNull();
    }
}