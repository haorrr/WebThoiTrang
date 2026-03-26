package com.fashionshop.dto.response;

import com.fashionshop.entity.Review;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReviewResponse {

    private Long id;
    private Long userId;
    private String userName;
    private String userAvatar;
    private Long productId;
    private Integer rating;
    private String comment;
    private String status;
    private LocalDateTime createdAt;

    public static ReviewResponse from(Review r) {
        return ReviewResponse.builder()
                .id(r.getId())
                .userId(r.getUser().getId())
                .userName(r.getUser().getName())
                .userAvatar(r.getUser().getAvatar())
                .productId(r.getProduct().getId())
                .rating(r.getRating())
                .comment(r.getComment())
                .status(r.getStatus().name())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
