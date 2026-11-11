package com.aquascope;

import com.aquascope.dto.ApiResponse;
import com.aquascope.dto.HealthResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AquaScopeApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Context Loads Successfully")
    void contextLoads() {
        assertThat(port).isPositive();
    }

    @Test
    @DisplayName("Health Endpoint Returns Success with HealthResponse DTO")
    void healthEndpointShouldReturnStatusUp() {
        String url = "http://localhost:" + port + "/api/v1/health";
        
        ResponseEntity<ApiResponse<HealthResponse>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        
        HealthResponse health = response.getBody().data();
        assertThat(health).isNotNull();
        assertThat(health.status()).isEqualTo("UP");
        assertThat(health.application()).isEqualTo("aquascope-backend-test");
        assertThat(health.database()).isNotNull();
        assertThat(health.database().connected()).isTrue();
    }
}
