package com.ivanfranchin.simpleservice;

import com.ivanfranchin.simpleservice.config.NativeRuntimeHintsRegistrar;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;

@ImportRuntimeHints(NativeRuntimeHintsRegistrar.class)
@SpringBootApplication
public class SimpleServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimpleServiceApplication.class, args);
    }
}
