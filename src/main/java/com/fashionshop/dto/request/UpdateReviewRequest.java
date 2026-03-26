package com.fashionshop.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateReviewRequest {

    @Min(value = 1) @Max(value = 5)
    private Integer rating;

    private String comment;
}
