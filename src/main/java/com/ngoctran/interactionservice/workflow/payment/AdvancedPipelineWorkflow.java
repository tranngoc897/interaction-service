package com.ngoctran.interactionservice.workflow.payment;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.util.List;
import java.util.Map;

@WorkflowInterface
public interface AdvancedPipelineWorkflow {

    /**
     * Thực thi một chuỗi các hoạt động dựa trên cấu hình động.
     * 
     * @param pipelineName Tên của pipeline (để log/tracking)
     * @param tasks        Danh sách các task cấu hình (name, activity, parallel,
     *                     retryOptions, v.v.)
     * @param inputData    Dữ liệu đầu vào ban đầu
     * @return Kết quả cuối cùng sau khi gộp từ tất cả các activities
     */
    @WorkflowMethod
    Map<String, Object> runPipeline(String pipelineName, List<Map<String, Object>> tasks,
            Map<String, Object> inputData);
}
