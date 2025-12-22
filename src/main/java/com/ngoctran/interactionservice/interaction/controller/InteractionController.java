package com.ngoctran.interactionservice.interaction.controller;

import com.ngoctran.interactionservice.ApiResponse;
import com.ngoctran.interactionservice.StepSubmissionDto;
import com.ngoctran.interactionservice.interaction.InteractionDto;
import com.ngoctran.interactionservice.interaction.InteractionStartRequest;
import com.ngoctran.interactionservice.interaction.service.InteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/interactions")
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;

    @PostMapping("/start")
    public ApiResponse<InteractionDto> start(@RequestBody InteractionStartRequest req) {
        return new ApiResponse<>("Interaction started", interactionService.startInteraction(req));
    }

    @PostMapping("/{interactionId}/steps")
    public ApiResponse<InteractionDto> submitStep(
            @PathVariable String interactionId,
            @RequestBody StepSubmissionDto dto) {
        return new ApiResponse<>("Step submitted", interactionService.submitStep(interactionId, dto));
    }

}

