package com.aquascope;

import com.aquascope.dto.*;
import com.aquascope.entity.Role;
import com.aquascope.entity.User;
import com.aquascope.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthenticationWorkflowTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1";
        userRepository.deleteAll();

        // Seed an admin user for testing
        User admin = new User(
                "testadmin",
                "admin@test.org",
                passwordEncoder.encode("Password123!"),
                "Test Administrator",
                Role.ADMINISTRATOR,
                "Quality Lab"
        );
        userRepository.save(admin);
    }

    @Test
    @DisplayName("Login with valid credentials returns JWT token")
    void loginWithValidCredentialsShouldReturnToken() {
        LoginRequest request = new LoginRequest("testadmin", "Password123!");
        ResponseEntity<ApiResponse<AuthResponse>> response = restTemplate.exchange(
                baseUrl + "/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data().token()).isNotBlank();
        assertThat(response.getBody().data().role()).isEqualTo(Role.ADMINISTRATOR);
    }

    @Test
    @DisplayName("Login with invalid password returns 401 Unauthorized")
    void loginWithInvalidPasswordShouldFail() {
        LoginRequest request = new LoginRequest("testadmin", "WrongPassword!");
        ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
                baseUrl + "/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
    }

    @Test
    @DisplayName("Accessing /auth/me without token returns 401 Unauthorized")
    void accessProfileWithoutTokenShouldBeRejected() {
        ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
                baseUrl + "/auth/me",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Accessing /auth/me with valid JWT token returns profile")
    void accessProfileWithValidTokenShouldSucceed() {
        // First login
        LoginRequest loginReq = new LoginRequest("testadmin", "Password123!");
        ResponseEntity<ApiResponse<AuthResponse>> loginResp = restTemplate.exchange(
                baseUrl + "/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginReq),
                new ParameterizedTypeReference<>() {}
        );
        String token = loginResp.getBody().data().token();

        // Now call /me
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<ApiResponse<UserDto>> profileResp = restTemplate.exchange(
                baseUrl + "/auth/me",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(profileResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(profileResp.getBody()).isNotNull();
        assertThat(profileResp.getBody().data().username()).isEqualTo("testadmin");
    }
}
