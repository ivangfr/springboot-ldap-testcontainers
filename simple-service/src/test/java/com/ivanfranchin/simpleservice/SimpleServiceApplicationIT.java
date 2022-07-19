package com.ivanfranchin.simpleservice;

import org.junit.jupiter.api.BeforeAll;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SimpleServiceApplicationIT {

    private static final String API_PUBLIC = "/api/public";
    private static final String API_PRIVATE = "/api/private";

    private static final String BGATES_VALID_USERNAME = "bgates";
    private static final String BGATES_VALID_PASSWORD = "123";

    private static final int OPENLDAP_EXPOSED_PORT = 389;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Container
    private static final GenericContainer<?> openldapContainer = new GenericContainer<>("osixia/openldap:1.5.0")
            .withNetworkAliases("openldap")
            .withEnv("LDAP_ORGANISATION", "MyCompany Inc.")
            .withEnv("LDAP_DOMAIN", "mycompany.com")
            .withExposedPorts(OPENLDAP_EXPOSED_PORT)
            .withFileSystemBind(
                    System.getProperty("user.dir") + "/src/main/resources/ldap-mycompany-com.ldif",
                    "/ldap/ldap-mycompany-com.ldif",
                    BindMode.READ_ONLY);

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        String openldapUrl = String.format("ldap://localhost:%s", openldapContainer.getMappedPort(OPENLDAP_EXPOSED_PORT));
        registry.add("spring.ldap.urls", () -> openldapUrl);
    }

    @BeforeAll
    static void beforeAll() throws IOException, InterruptedException {
        openldapContainer.execInContainer("ldapadd", "-x", "-D", "cn=admin,dc=mycompany,dc=com", "-w", "admin", "-H", "ldap://", "-f", "ldap/ldap-mycompany-com.ldif");
    }

    @Test
    void testGetPublicString() {
        ResponseEntity<String> responseEntity = testRestTemplate.getForEntity(API_PUBLIC, String.class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody()).isEqualTo("It is public.");
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
        assertThat(responseEntity.getBody()).isEqualTo("bgates, it is private.");
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
}
