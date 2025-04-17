package com.mbzuai.ams.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class SubmissionResponse {
    private Long id;
    private Long assignmentId;
    private Long studentId;
    private String studentName;
    private String fileName;
    private LocalDateTime submittedAt;
    private Integer grade;
    private String feedback;
    private LocalDateTime gradedAt;
}
