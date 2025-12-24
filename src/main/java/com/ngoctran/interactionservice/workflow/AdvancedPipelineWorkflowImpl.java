package com.ngoctran.interactionservice.workflow;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.ActivityStub;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class AdvancedPipelineWorkflowImpl implements AdvancedPipelineWorkflow {

    private static final Logger log = Workflow.getLogger(AdvancedPipelineWorkflowImpl.class);

    @Override
    public Map<String, Object> runPipeline(String pipelineName, List<Map<String, Object>> tasks,
            Map<String, Object> inputData) {
        log.info("Starting Advanced Pipeline: {} with {} tasks", pipelineName, tasks.size());

        Map<String, Object> sharedContext = new HashMap<>(inputData);

        // Group tasks into batches: sequential steps or parallel groups
        // For simplicity in this example, we iterate and check 'isParallel' flag

        int i = 0;
        while (i < tasks.size()) {
            Map<String, Object> taskConfig = tasks.get(i);
            boolean isParallel = Boolean.TRUE.equals(taskConfig.get("parallel"));

            if (!isParallel) {
                // 1. Thực thi Tuần tự (Sequential)
                executeSingleTask(taskConfig, sharedContext);
                i++;
            } else {
                // 2. Thực thi Song song (Parallel) - Gộp các task liên tiếp có cờ parallel=true
                List<Map<String, Object>> parallelGroup = new ArrayList<>();
                while (i < tasks.size() && Boolean.TRUE.equals(tasks.get(i).get("parallel"))) {
                    parallelGroup.add(tasks.get(i));
                    i++;
                }
                executeParallelTasks(parallelGroup, sharedContext);
            }
        }

        log.info("Pipeline {} completed successfully", pipelineName);
        return sharedContext;
    }

    private void executeSingleTask(Map<String, Object> config, Map<String, Object> context) {
        String activityName = (String) config.get("activity");
        if (activityName == null)
            return;

        log.info("Executing Sequential Activity: {}", activityName);

        ActivityStub stub = createUntypedStub(config);

        try {
            // Thực thi activity không định kiểu
            // Giả định input là (String caseId, Map context) dựa trên pattern cũ
            // Hoặc có thể cấu hình input linh hoạt hơn
            @SuppressWarnings("unchecked")
            Map<String, Object> result = stub.execute(
                    activityName,
                    Map.class,
                    context.getOrDefault("caseId", "DEFAULT_CASE"),
                    context);

            if (result != null) {
                context.putAll(result);
            }
        } catch (Exception e) {
            log.error("Task {} failed", activityName, e);
            if (Boolean.TRUE.equals(config.get("continueOnError"))) {
                log.warn("Continuing pipeline due to continueOnError=true");
            } else {
                throw e;
            }
        }
    }

    private void executeParallelTasks(List<Map<String, Object>> taskConfigs, Map<String, Object> context) {
        log.info("Executing {} tasks in parallel", taskConfigs.size());

        List<Promise<Map<String, Object>>> promises = taskConfigs.stream().map(config -> {
            String activityName = (String) config.get("activity");
            ActivityStub stub = createUntypedStub(config);

            // Dùng executeAsync để chạy song song
            @SuppressWarnings({ "unchecked", "rawtypes" })
            Promise<Map<String, Object>> promise = (Promise) stub.executeAsync(
                    activityName,
                    Map.class,
                    context.getOrDefault("caseId", "DEFAULT_CASE"),
                    context);
            return promise;
        }).collect(Collectors.toList());

        // Đợi tất cả hoàn thành
        Promise.allOf(promises).get();

        // Gộp kết quả
        for (Promise<Map<String, Object>> promise : promises) {
            Map<String, Object> result = promise.get();
            if (result != null) {
                context.putAll(result);
            }
        }
    }

    private ActivityStub createUntypedStub(Map<String, Object> config) {
        // Có thể lấy timeout/retry từ config nếu có
        int timeoutSeconds = (int) config.getOrDefault("timeout", 60);

        ActivityOptions options = ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(timeoutSeconds))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setMaximumAttempts(3)
                        .build())
                .build();

        return Workflow.newUntypedActivityStub(options);
    }
}
