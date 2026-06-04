package com.myrtletrip.round.exceptionmodel.controller;

import com.myrtletrip.round.exceptionmodel.dto.RoundTeamExceptionPageResponse;
import com.myrtletrip.round.exceptionmodel.dto.RoundTeamExceptionRequest;
import com.myrtletrip.round.exceptionmodel.dto.RoundTeamExceptionResponse;
import com.myrtletrip.round.exceptionmodel.service.RoundTeamExceptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rounds/{roundId}/team-exceptions")
public class RoundTeamExceptionController {

    private final RoundTeamExceptionService service;

    public RoundTeamExceptionController(RoundTeamExceptionService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<RoundTeamExceptionPageResponse> getExceptions(@PathVariable Long roundId) {
        return ResponseEntity.ok(service.getExceptions(roundId));
    }

    @PostMapping
    public ResponseEntity<RoundTeamExceptionResponse> saveException(
            @PathVariable Long roundId,
            @RequestBody RoundTeamExceptionRequest request
    ) {
        return ResponseEntity.ok(service.saveException(roundId, request));
    }

    @DeleteMapping("/{exceptionId}")
    public ResponseEntity<Void> deleteException(@PathVariable Long roundId, @PathVariable Long exceptionId) {
        service.deactivateException(roundId, exceptionId);
        return ResponseEntity.noContent().build();
    }
}
