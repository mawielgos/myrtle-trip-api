package com.myrtletrip.playerimport.dto;

import com.myrtletrip.playerimport.model.ImportMatchStatus;

import java.math.BigDecimal;

public class PlayerImportRow {

    private Integer rowNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String ghinNumber;
    private String gender;
    private BigDecimal handicapIndex;

    private ImportMatchStatus matchStatus;

    private Long matchedPlayerId;
    private String matchedPlayerName;

    private Boolean alreadyInTrip;
    private String validationMessage;

    public Integer getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(Integer rowNumber) {
        this.rowNumber = rowNumber;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGhinNumber() {
        return ghinNumber;
    }

    public void setGhinNumber(String ghinNumber) {
        this.ghinNumber = ghinNumber;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public BigDecimal getHandicapIndex() {
        return handicapIndex;
    }

    public void setHandicapIndex(BigDecimal handicapIndex) {
        this.handicapIndex = handicapIndex;
    }

    public ImportMatchStatus getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(ImportMatchStatus matchStatus) {
        this.matchStatus = matchStatus;
    }

    public Long getMatchedPlayerId() {
        return matchedPlayerId;
    }

    public void setMatchedPlayerId(Long matchedPlayerId) {
        this.matchedPlayerId = matchedPlayerId;
    }

    public String getMatchedPlayerName() {
        return matchedPlayerName;
    }

    public void setMatchedPlayerName(String matchedPlayerName) {
        this.matchedPlayerName = matchedPlayerName;
    }

    public Boolean getAlreadyInTrip() {
        return alreadyInTrip;
    }

    public void setAlreadyInTrip(Boolean alreadyInTrip) {
        this.alreadyInTrip = alreadyInTrip;
    }

    public String getValidationMessage() {
        return validationMessage;
    }

    public void setValidationMessage(String validationMessage) {
        this.validationMessage = validationMessage;
    }
}
