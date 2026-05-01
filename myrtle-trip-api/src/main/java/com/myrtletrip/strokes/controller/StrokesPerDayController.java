package com.myrtletrip.strokes.controller;

import com.myrtletrip.strokes.dto.StrokesPerDayResponse;
import com.myrtletrip.strokes.service.StrokesPerDayService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips/{tripId}/strokes-per-day")
public class StrokesPerDayController {

    private final StrokesPerDayService strokesPerDayService;

    public StrokesPerDayController(StrokesPerDayService strokesPerDayService) {
        this.strokesPerDayService = strokesPerDayService;
    }

    @GetMapping
    public StrokesPerDayResponse getStrokesPerDay(@PathVariable Long tripId) {
        return strokesPerDayService.getStrokesPerDay(tripId);
    }
}
