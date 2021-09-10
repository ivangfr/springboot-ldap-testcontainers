package com.mycompany.simpleservice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SimpleServiceApplicationIT extends AbstractTestcontainers {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    void testGetPublicString() {
        System.out.println(System.getProperties());

        ResponseEntity<String> responseEntity = testRestTemplate.getForEntity(API_PUBLIC, String.class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody()).isEqualTo("It is public.\n");
    }

    @Test
    void testGetPrivateStringWithoutAuthentication() {
        ResponseEntity<String> responseEntity = testRestTemplate.getForEntity(API_PRIVATE, String.class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testGetPrivateStringWithValidCredentials() {
        ResponseEntity<String> responseEntity = testRestTemplate
                .withBasicAuth(BGATES_VALID_USERNAME, BGATES_VALID_PASSWORD)
                .getForEntity(API_PRIVATE, String.class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody()).isEqualTo("bgates, it is private.\n");
    }

    @ParameterizedTest
    @MethodSource("provideInvalidCredentials")
    void testGetPrivateStringWithInvalidCredentials(String username, String password) {
        ResponseEntity<String> responseEntity = testRestTemplate
                .withBasicAuth(username, password)
                .getForEntity(API_PRIVATE, String.class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private static Stream<Arguments> provideInvalidCredentials() {
        return Stream.of(
                Arguments.of("", ""),
                Arguments.of(" ", " "),
                Arguments.of(BGATES_VALID_USERNAME, "invalid_password"),
                Arguments.of("invalid_username", BGATES_VALID_PASSWORD)
        );
    }

    private static final String API_PUBLIC = "/api/public";
    private static final String API_PRIVATE = "/api/private";

    private static final String BGATES_VALID_USERNAME = "bgates";
    private static final String BGATES_VALID_PASSWORD = "123";
}
