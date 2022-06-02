package com.mycompany.simpleservice;

import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.DecoratingProxy;
import org.springframework.nativex.hint.JdkProxyHint;
import org.springframework.security.authentication.AuthenticationManager;

@JdkProxyHint(types = {AuthenticationManager.class, SpringProxy.class, Advised.class, DecoratingProxy.class})
@SpringBootApplication
public class SimpleServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimpleServiceApplication.class, args);
    }
}
