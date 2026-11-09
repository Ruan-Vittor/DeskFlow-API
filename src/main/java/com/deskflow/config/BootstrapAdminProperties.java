package com.deskflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "deskflow.bootstrap.admin")
public record BootstrapAdminProperties(String name, String email, String password) {
}
