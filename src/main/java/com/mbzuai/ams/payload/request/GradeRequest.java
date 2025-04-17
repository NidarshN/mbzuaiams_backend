package com.mbzuai.ams.payload.request;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Data
public class GradeRequest {
    @NotNull
    @Min(0)
    @Max(100)
    private Integer grade;

    private String feedback;
}