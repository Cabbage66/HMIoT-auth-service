package com.hmiot.hmiot_backend.module.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("identity_ticket")
public class IdentityTicket {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String ticketId;
    private Long userId;
    private Long terminalId;
    private String issuerSystem;
    private String audienceSystem;
    private Integer trustLevel;
    private BigDecimal trustScore;
    private String signatureDigest;
    private String ticketPayload;
    private String status;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(Long terminalId) {
        this.terminalId = terminalId;
    }

    public String getIssuerSystem() {
        return issuerSystem;
    }

    public void setIssuerSystem(String issuerSystem) {
        this.issuerSystem = issuerSystem;
    }

    public String getAudienceSystem() {
        return audienceSystem;
    }

    public void setAudienceSystem(String audienceSystem) {
        this.audienceSystem = audienceSystem;
    }

    public Integer getTrustLevel() {
        return trustLevel;
    }

    public void setTrustLevel(Integer trustLevel) {
        this.trustLevel = trustLevel;
    }

    public BigDecimal getTrustScore() {
        return trustScore;
    }

    public void setTrustScore(BigDecimal trustScore) {
        this.trustScore = trustScore;
    }

    public String getSignatureDigest() {
        return signatureDigest;
    }

    public void setSignatureDigest(String signatureDigest) {
        this.signatureDigest = signatureDigest;
    }

    public String getTicketPayload() {
        return ticketPayload;
    }

    public void setTicketPayload(String ticketPayload) {
        this.ticketPayload = ticketPayload;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }
}
