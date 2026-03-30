package com.fashionshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryTreeResponse {

    private Long id;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private String status;
    private Long parentId;
    private int productCount;
    private List<CategoryTreeResponse> children;
}
