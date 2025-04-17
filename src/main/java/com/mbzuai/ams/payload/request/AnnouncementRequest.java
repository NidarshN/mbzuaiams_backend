package com.mbzuai.ams.payload.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class AnnouncementRequest {
    @NotBlank
    private String title;

    @NotBlank
    private String content;
}
