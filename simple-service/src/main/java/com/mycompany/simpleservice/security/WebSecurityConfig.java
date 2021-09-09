package com.mycompany.simpleservice.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ldap.LdapProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(LdapProperties.class)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${ldap.userDnPattern}")
    private String userDnPattern;

    private final LdapProperties ldapProperties;

    public WebSecurityConfig(LdapProperties ldapProperties) {
        this.ldapProperties = ldapProperties;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers(HttpMethod.GET, "/api/private").authenticated()
                .antMatchers(HttpMethod.GET, "/api/public").permitAll()
                .antMatchers(HttpMethod.GET, "/actuator/**").permitAll()
                .antMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
                .and()
                .httpBasic();
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        http.cors().and().csrf().disable();
    }

    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        String url = String.format("%s/%s", ldapProperties.getUrls()[0], ldapProperties.getBase());

        auth.ldapAuthentication()
                .contextSource()
                .url(url)
                .managerDn(ldapProperties.getUsername())
                .managerPassword(ldapProperties.getPassword())
                .and()
                .userDnPatterns(userDnPattern);
    }
}
