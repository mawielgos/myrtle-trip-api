package com.myrtletrip.player.dto;

public class PlayerDetailResponse {

    private Long playerId;
    private String firstName;
    private String lastName;
    private String displayName;
    private String ghinNumber;
    private Boolean active;
    private String email;
    private String cell;
    private String venmoId;
    private String zelleId;
    private String handicapMethod;
    private String gender;

    public PlayerDetailResponse() {
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getGhinNumber() {
        return ghinNumber;
    }

    public void setGhinNumber(String ghinNumber) {
        this.ghinNumber = ghinNumber;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCell() {
        return cell;
    }

    public void setCell(String cell) {
        this.cell = cell;
    }

    public String getVenmoId() {
        return venmoId;
    }

    public void setVenmoId(String venmoId) {
        this.venmoId = venmoId;
    }

    public String getZelleId() {
        return zelleId;
    }

    public void setZelleId(String zelleId) {
        this.zelleId = zelleId;
    }

    public String getHandicapMethod() {
        return handicapMethod;
    }

    public void setHandicapMethod(String handicapMethod) {
        this.handicapMethod = handicapMethod;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
}

