package com.fashionshop.dto.response;

import com.fashionshop.entity.Review;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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
    private List<String> imageUrls;
    private LocalDateTime createdAt;

    public static ReviewResponse from(Review r) {
        List<String> imgs = r.getImages().stream()
                .sorted(java.util.Comparator.comparingInt(i -> i.getSortOrder()))
                .map(i -> i.getImageUrl())
                .toList();
        return ReviewResponse.builder()
                .id(r.getId())
                .userId(r.getUser().getId())
                .userName(r.getUser().getName())
                .userAvatar(r.getUser().getAvatar())
                .productId(r.getProduct().getId())
                .rating(r.getRating())
                .comment(r.getComment())
                .status(r.getStatus().name())
                .imageUrls(imgs)
                .createdAt(r.getCreatedAt())
                .build();
    }
}
