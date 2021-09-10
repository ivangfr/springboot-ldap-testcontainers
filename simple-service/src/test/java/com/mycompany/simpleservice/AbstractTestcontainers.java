package com.mycompany.simpleservice;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;

@Testcontainers
public class AbstractTestcontainers {

    private static final GenericContainer<?> openldapContainer = new GenericContainer<>("osixia/openldap:1.5.0");

    private static final int OPENLDAP_EXPOSED_PORT = 389;

    @DynamicPropertySource
    private static void dynamicProperties(DynamicPropertyRegistry registry) throws IOException, InterruptedException {
        String hostPath = System.getProperty("user.dir") + "/src/main/resources/ldap-mycompany-com.ldif";
        String containerPath = "/ldap/ldap-mycompany-com.ldif";

        openldapContainer.withNetworkAliases("openldap")
                .withEnv("LDAP_ORGANISATION", "MyCompany Inc.")
                .withEnv("LDAP_DOMAIN", "mycompany.com")
                .withExposedPorts(OPENLDAP_EXPOSED_PORT)
                .withFileSystemBind(hostPath, containerPath, BindMode.READ_ONLY)
                .start();

        openldapContainer.execInContainer("ldapadd", "-x", "-D", "cn=admin,dc=mycompany,dc=com", "-w", "admin", "-H", "ldap://", "-f", "ldap/ldap-mycompany-com.ldif");

        String openldapUrl = String.format("ldap://localhost:%s", openldapContainer.getMappedPort(OPENLDAP_EXPOSED_PORT));
        registry.add("spring.ldap.urls", () -> openldapUrl);
    }
}
