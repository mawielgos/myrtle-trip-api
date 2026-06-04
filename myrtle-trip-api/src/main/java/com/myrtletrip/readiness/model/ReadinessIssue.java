package com.myrtletrip.readiness.model;

public class ReadinessIssue {

    private String code;
    private ReadinessSeverity severity;
    private String area;
    private String message;

    public ReadinessIssue() {
    }

    public ReadinessIssue(String code, ReadinessSeverity severity, String area, String message) {
        this.code = code;
        this.severity = severity;
        this.area = area;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public ReadinessSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(ReadinessSeverity severity) {
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
}
