package com.mycompany.springbootldap.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

@Data
@ConfigurationProperties(prefix="spring.ldap")
@Validated
public class LdapProperties {

    @NotNull
    private String urls;

    @NotNull
    private Base base;

    @NotNull
    private Manager manager;

    @NotNull
    private User user;

    @Data
    public static class Base {

        @NotNull
        private String dn;
    }

    @Data
    public static class Manager {

        @NotNull
        private String dn;

        @NotNull
        private String password;
    }

    @Data
    public static class User {

        @NotNull
        private Dn dn;

        @Data
        public static class Dn {

            @NotNull
            private String pattern;
        }
    }

}
