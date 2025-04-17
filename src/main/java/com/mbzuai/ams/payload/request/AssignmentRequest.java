package com.mbzuai.ams.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AssignmentRequest {
    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotNull
    private LocalDateTime dueDate;
}
