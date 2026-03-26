package com.fashionshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name too long")
    private String name;

    private String description;
    private String imageUrl;
    private Long parentId;
}
