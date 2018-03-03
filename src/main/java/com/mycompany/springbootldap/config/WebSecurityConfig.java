package com.mycompany.springbootldap.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${ldap.urls}")
    private String ldapUrls;

    @Value("${ldap.base.dn}")
    private String ldapBaseDn;

    @Value("${ldap.manager.dn}")
    private String ldapManagerDn;

    @Value("${ldap.manager.password}")
    private String ldapManagerPassword;

    @Value("${ldap.user.dn.pattern}")
    private String ldapUserDnPattern;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()//
                .antMatchers("/api/private**").authenticated()//
                .anyRequest().permitAll()//
                .and()//
                .httpBasic();
    }

    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.ldapAuthentication().contextSource()//
                .url(ldapUrls + ldapBaseDn)//
                .managerDn(ldapManagerDn)//
                .managerPassword(ldapManagerPassword)//
                .and()//
                .userDnPatterns(ldapUserDnPattern);
    }

}
