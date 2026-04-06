package com.myrtletrip.skins.controller;

import com.myrtletrip.skins.dto.SkinsResultResponse;
import com.myrtletrip.skins.service.SkinsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/skins")
public class SkinsController {

    private final SkinsService skinsService;

    public SkinsController(SkinsService skinsService) {
        this.skinsService = skinsService;
    }

    @GetMapping("/rounds/{roundId}/net")
    public SkinsResultResponse getNetSkins(@PathVariable Long roundId) {
        return skinsService.getNetSkins(roundId);
    }
}