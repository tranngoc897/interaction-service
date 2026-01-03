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

    @PostMapping("/{caseId}/start-process")
    public ResponseEntity<Void> startProcess(
            @PathVariable UUID caseId,
            @RequestParam String processDefinitionKey,
            @RequestBody(required = false) Map<String, Object> variables) {
        myCaseService.startBpmnProcess(caseId, processDefinitionKey, variables);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{caseId}/history")
    public ResponseEntity<?> getHistory(@PathVariable UUID caseId) {
        CaseEntity caseEntity = myCaseService.getCase(caseId);
        return ResponseEntity.ok(caseEntity.getAuditTrail());
    }

    @GetMapping("/definitions/{key}/schema")
    public ResponseEntity<String> getCaseSchema(@PathVariable String key) {
        return ResponseEntity.ok(myCaseService.getCaseSchema(key));
    }

    @PostMapping("/migrate")
    public ResponseEntity<?> migrateCases(@RequestBody Map<String, Object> request) {
        String sourceDefId = (String) request.get("sourceDefId");
        String targetDefId = (String) request.get("targetDefId");
        List<String> caseIds = (List<String>) request.get("caseIds");
        return ResponseEntity.ok(myCaseService.migrateBpmVersion(sourceDefId, targetDefId, caseIds));
    }
}
