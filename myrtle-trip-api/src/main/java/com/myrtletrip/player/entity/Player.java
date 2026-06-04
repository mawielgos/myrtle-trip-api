package com.myrtletrip.player.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "player")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "ghin_number", length = 25)
    private String ghinNumber;

    private boolean active;

    @Column(name = "email")
    private String email;

    @Column(name = "cell")
    private String cell;

    @Column(name = "venmo_id")
    private String venmoId;

    @Column(name = "zelle_id")
    private String zelleId;

    @Column(name = "legacy_player_number")
    private Integer legacyPlayerNumber;

    @Column(name = "handicap_method", length = 30)
    private String handicapMethod;

    @Column(name = "gender", length = 1, nullable = false)
    private String gender = "M";

    @Column(name = "created_source", length = 25)
    private String createdSource;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "normalized_name", length = 200)
    private String normalizedName;

    @Column(name = "normalized_email", length = 200)
    private String normalizedEmail;

    @PrePersist
    @PreUpdate
    public void normalizeFields() {
        this.normalizedName = normalize(firstName + " " + lastName);
        this.normalizedEmail = normalizeEmail(email);
    }

    private String normalizeEmail(String value) {
        String cleaned = cleanEmail(value);

        if (cleaned == null) {
            return null;
        }

        return cleaned.trim().toLowerCase();
    }

    private String cleanEmail(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.trim();

        if (cleaned.isEmpty()) {
            return null;
        }

        if (cleaned.toLowerCase().startsWith("mailto:")) {
            cleaned = cleaned.substring(7).trim();
        }

        int openAngle = cleaned.indexOf('<');
        int closeAngle = cleaned.indexOf('>');
        if (openAngle >= 0 && closeAngle > openAngle) {
            cleaned = cleaned.substring(openAngle + 1, closeAngle).trim();
        }

        int semicolon = cleaned.indexOf(';');
        if (semicolon >= 0) {
            cleaned = cleaned.substring(0, semicolon).trim();
        }

        cleaned = cleaned.replaceAll("^[\\s\"'<>;,]+", "");
        cleaned = cleaned.replaceAll("[\\s\"'<>;,]+$", "");

        return cleaned.isEmpty() ? null : cleaned;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        return value.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getGhinNumber() { return ghinNumber; }
    public void setGhinNumber(String ghinNumber) { this.ghinNumber = ghinNumber; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCell() { return cell; }
    public void setCell(String cell) { this.cell = cell; }

    public String getVenmoId() { return venmoId; }
    public void setVenmoId(String venmoId) { this.venmoId = venmoId; }

    public String getZelleId() { return zelleId; }
    public void setZelleId(String zelleId) { this.zelleId = zelleId; }

    public Integer getLegacyPlayerNumber() { return legacyPlayerNumber; }
    public void setLegacyPlayerNumber(Integer legacyPlayerNumber) { this.legacyPlayerNumber = legacyPlayerNumber; }

    public String getHandicapMethod() { return handicapMethod; }
    public void setHandicapMethod(String handicapMethod) { this.handicapMethod = handicapMethod; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getCreatedSource() { return createdSource; }
    public void setCreatedSource(String createdSource) { this.createdSource = createdSource; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getNormalizedName() { return normalizedName; }
    public void setNormalizedName(String normalizedName) { this.normalizedName = normalizedName; }

    public String getNormalizedEmail() { return normalizedEmail; }
    public void setNormalizedEmail(String normalizedEmail) { this.normalizedEmail = normalizedEmail; }
}
