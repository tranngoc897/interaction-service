package com.ngoctran.interactionservice.cases;

import com.ngoctran.interactionservice.dto.NextStepResponse;
import com.ngoctran.interactionservice.dto.StepSubmissionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/cases")
@RequiredArgsConstructor
public class CaseController {

    private final MyCaseService myCaseService;

    @PostMapping
    public ResponseEntity<?> createCase(@RequestBody(required = false) Map<String, Object> initialData) {
        var caseId = myCaseService.createCase(initialData);
        return ResponseEntity.ok(Map.of("caseId", caseId));
    }

    @GetMapping
    public ResponseEntity<List<CaseEntity>> listCases(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(myCaseService.listCases(customerId, status));
    }

    @GetMapping("/{caseId}")
    public ResponseEntity<CaseEntity> getCase(@PathVariable UUID caseId) {
        return ResponseEntity.ok(myCaseService.getCase(caseId));
    }

    @PostMapping("/{caseId}/steps")
    public ResponseEntity<NextStepResponse> submitStep(
            @PathVariable UUID caseId,
            @RequestBody StepSubmissionDto submission) {
        return ResponseEntity.ok(myCaseService.submitStep(caseId, submission));
    }

    @GetMapping("/{caseId}/tasks")
    public ResponseEntity<?> getTasks(@PathVariable UUID caseId) {
        return ResponseEntity.ok(myCaseService.getTasksByCase(caseId));
    }

    @PostMapping("/{caseId}/cancel")
    public ResponseEntity<?> cancelCase(@PathVariable UUID caseId) {
        myCaseService.cancelCase(caseId);
        return ResponseEntity.ok(Map.of("message", "Case cancelled successfully"));
    }

    @GetMapping("/{caseId}/history")
    public ResponseEntity<?> getHistory(@PathVariable UUID caseId) {
        CaseEntity caseEntity = myCaseService.getCase(caseId);
        return ResponseEntity.ok(caseEntity.getAuditTrail());
    }
}
