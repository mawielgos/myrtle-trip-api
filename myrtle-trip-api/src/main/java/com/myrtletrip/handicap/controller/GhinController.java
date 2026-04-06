package com.myrtletrip.handicap.controller;

import com.myrtletrip.handicap.service.FrozenGhinImportService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ghin")
public class GhinController {

    private final FrozenGhinImportService service;

    public GhinController(FrozenGhinImportService service) {
        this.service = service;
    }

    @PostMapping("/initialize")
    public String initialize(@RequestParam String groupCode) throws Exception {
        service.initializeFrozenGhinForGroup(groupCode);
        return "GHIN import completed for " + groupCode;
    }
}