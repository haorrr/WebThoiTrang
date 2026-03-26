package com.fashionshop.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CategoryTreeResponse {

    private Long id;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private String status;
    private List<CategoryTreeResponse> children;
}
