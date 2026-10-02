package com.myrtletrip.strokes.service;

import com.myrtletrip.strokes.dto.StrokesPerDayResponse;
import com.myrtletrip.strokes.dto.StrokesPerDayTeePlanSaveRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StrokesPerDayService {

    private final StrokesPerDayReadModelService readModelService;
    private final StrokesPerDayTeePlanCommandService teePlanCommandService;

    public StrokesPerDayService(StrokesPerDayReadModelService readModelService,
                                StrokesPerDayTeePlanCommandService teePlanCommandService) {
        this.readModelService = readModelService;
        this.teePlanCommandService = teePlanCommandService;
    }

    public StrokesPerDayResponse getStrokesPerDay(Long tripId) {
        return readModelService.getStrokesPerDay(tripId);
    }

    @Transactional
    public StrokesPerDayResponse saveTeePlan(Long tripId, StrokesPerDayTeePlanSaveRequest request) {
        teePlanCommandService.saveTeePlan(tripId, request);
        return readModelService.getStrokesPerDay(tripId);
    }
}
