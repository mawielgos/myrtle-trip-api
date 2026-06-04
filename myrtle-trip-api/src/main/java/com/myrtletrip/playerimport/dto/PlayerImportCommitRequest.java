package com.myrtletrip.playerimport.dto;

import java.util.List;

public class PlayerImportCommitRequest {

    private List<PlayerImportRow> rows;

    public List<PlayerImportRow> getRows() {
        return rows;
    }

    public void setRows(List<PlayerImportRow> rows) {
        this.rows = rows;
    }
}
