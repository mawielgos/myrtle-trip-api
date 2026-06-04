package com.myrtletrip.readiness.dto;

public class ReadinessIssueResponse {

    private String code;
    private String severity;
    private String area;
    private String message;
    private String actionLabel;
    private String actionPath;

    public ReadinessIssueResponse() {
    }

    public ReadinessIssueResponse(String code, String severity, String area, String message) {
        this.code = code;
        this.severity = severity;
        this.area = area;
        this.message = message;
    }

    public ReadinessIssueResponse(String code, String severity, String area, String message, String actionLabel, String actionPath) {
        this.code = code;
        this.severity = severity;
        this.area = area;
        this.message = message;
        this.actionLabel = actionLabel;
        this.actionPath = actionPath;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getActionLabel() {
        return actionLabel;
    }

    public void setActionLabel(String actionLabel) {
        this.actionLabel = actionLabel;
    }

    public String getActionPath() {
        return actionPath;
    }

    public void setActionPath(String actionPath) {
        this.actionPath = actionPath;
    }
}
