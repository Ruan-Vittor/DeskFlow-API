package com.deskflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.deskflow.config")
public class DeskFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeskFlowApplication.class, args);
    }
}
