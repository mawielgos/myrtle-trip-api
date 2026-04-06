package com.myrtletrip.contest.controller;

import com.myrtletrip.contest.dto.TwoManBestBallTeamResponse;
import com.myrtletrip.contest.service.ContestService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/contests")
public class ContestController {

    private final ContestService contestService;

    public ContestController(ContestService contestService) {
        this.contestService = contestService;
    }

    @GetMapping("/rounds/{roundId}/two-man-best-ball")
    public List<TwoManBestBallTeamResponse> getTwoManBestBall(@PathVariable Long roundId) {
        return contestService.getTwoManBestBallResults(roundId);
    }
}