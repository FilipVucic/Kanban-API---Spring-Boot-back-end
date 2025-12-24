package com.hivetech.kanban.service;

import com.hivetech.kanban.entity.User;
import com.hivetech.kanban.exception.ResourceAlreadyExistsException;
import com.hivetech.kanban.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User savedUser;

    @BeforeEach
    void setUp() {
        savedUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("encodedPassword")
                .role("USER")
                .build();
    }

    @Test
    @DisplayName("should register new user successfully")
    void shouldRegisterNewUserSuccessfully() {
        // given
        String username = "testuser";
        String password = "password123";
        
        given(userRepository.existsByUsername(username)).willReturn(false);
        given(passwordEncoder.encode(password)).willReturn("encodedPassword");
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        // when
        User result = userService.register(username, password);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(username);
        assertThat(result.getRole()).isEqualTo("USER");
        assertThat(result.getPassword()).isEqualTo("encodedPassword");
    }

    @Test
    @DisplayName("should throw exception when username already exists")
    void shouldThrowExceptionWhenUsernameAlreadyExists() {
        // given
        String username = "existinguser";
        String password = "password123";
        
        given(userRepository.existsByUsername(username)).willReturn(true);

        // when/then
        assertThatThrownBy(() -> userService.register(username, password))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("already exists");
    }
}

