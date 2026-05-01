package com.myrtletrip.round.dto;

public class RoundCorrectionResponse {

    private boolean success;
    private String message;

    public RoundCorrectionResponse() {}

    public RoundCorrectionResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
