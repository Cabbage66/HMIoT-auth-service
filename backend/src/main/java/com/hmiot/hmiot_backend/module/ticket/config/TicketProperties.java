package com.hmiot.hmiot_backend.module.ticket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "hmiot.ticket")
public class TicketProperties {
    private String issuerSystem = "HMIOT_AUTH_SERVICE";
    private String signingSecret = "dev-only-hmiot-ticket-signing-secret-change-before-production";
    private Duration expiration = Duration.ofMinutes(30);
    private Duration refreshWindow = Duration.ofMinutes(10);

    public String getIssuerSystem() {
        return issuerSystem;
    }

    public void setIssuerSystem(String issuerSystem) {
        this.issuerSystem = issuerSystem;
    }

    public String getSigningSecret() {
        return signingSecret;
    }

    public void setSigningSecret(String signingSecret) {
        this.signingSecret = signingSecret;
    }

    public Duration getExpiration() {
        return expiration;
    }

    public void setExpiration(Duration expiration) {
        this.expiration = expiration;
    }

    public Duration getRefreshWindow() {
        return refreshWindow;
    }

    public void setRefreshWindow(Duration refreshWindow) {
        this.refreshWindow = refreshWindow;
    }
}
