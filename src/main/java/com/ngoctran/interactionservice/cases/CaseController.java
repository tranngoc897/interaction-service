package com.ngoctran.interactionservice.cases;


import com.ngoctran.interactionservice.NextStepResponse;
import com.ngoctran.interactionservice.StepSubmissionDto;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/cases")
public class CaseController {

    private final CaseService caseService;

    public CaseController(CaseService caseService){ this.caseService = caseService; }

    @PostMapping
    public ResponseEntity<?> createCase(@RequestBody(required = false) Map<String,Object> initialData) {
        var caseId = caseService.createCase(initialData);
        return ResponseEntity.ok(Map.of("caseId", caseId));
    }

    @GetMapping("/{caseId}")
    public ResponseEntity<?> getCase(@PathVariable UUID caseId){
        return ResponseEntity.ok(caseService.getCase(caseId));
    }

    @PostMapping("/{caseId}/steps")
    public ResponseEntity<NextStepResponse> submitStep(
        @PathVariable UUID caseId,
        @RequestBody StepSubmissionDto submission
    ) {
        return ResponseEntity.ok(caseService.submitStep(caseId, submission));
    }
}