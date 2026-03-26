package com.fashionshop.service;

import com.fashionshop.dto.request.CreateReviewRequest;
import com.fashionshop.dto.request.UpdateReviewRequest;
import com.fashionshop.dto.response.ReviewResponse;
import com.fashionshop.entity.Product;
import com.fashionshop.entity.Review;
import com.fashionshop.entity.User;
import com.fashionshop.exception.BadRequestException;
import com.fashionshop.exception.ResourceNotFoundException;
import com.fashionshop.repository.ProductRepository;
import com.fashionshop.repository.ReviewRepository;
import com.fashionshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public Page<ReviewResponse> getProductReviews(Long productId, boolean approvedOnly, int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (approvedOnly) {
            return reviewRepository.findByProductIdAndStatus(productId, Review.Status.APPROVED, pr)
                    .map(ReviewResponse::from);
        }
        return reviewRepository.findByProductId(productId, pr).map(ReviewResponse::from);
    }

    public Page<ReviewResponse> getUserReviews(Long userId, int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return reviewRepository.findByUserId(userId, pr).map(ReviewResponse::from);
    }

    public Page<ReviewResponse> getAllReviews(String status, int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (status != null) {
            Review.Status s = Review.Status.valueOf(status.toUpperCase());
            return reviewRepository.findByStatus(s, pr).map(ReviewResponse::from);
        }
        return reviewRepository.findAll(pr).map(ReviewResponse::from);
    }

    @Transactional
    public ReviewResponse createReview(Long userId, CreateReviewRequest req) {
        if (reviewRepository.existsByUserIdAndProductId(userId, req.getProductId())) {
            throw new BadRequestException("You have already reviewed this product");
        }

        Product product = productRepository.findById(req.getProductId())
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Product", req.getProductId()));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(req.getRating())
                .comment(req.getComment())
                .build();

        return ReviewResponse.from(reviewRepository.save(review));
    }

    @Transactional
    public ReviewResponse updateReview(Long userId, Long reviewId, UpdateReviewRequest req) {
        Review review = reviewRepository.findByIdAndUserId(reviewId, userId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));

        if (req.getRating() != null) review.setRating(req.getRating());
        if (req.getComment() != null) review.setComment(req.getComment());
        review.setStatus(Review.Status.PENDING); // re-moderate on edit

        return ReviewResponse.from(reviewRepository.save(review));
    }

    @Transactional
    public void deleteReview(Long userId, Long reviewId, boolean isAdmin) {
        Review review;
        if (isAdmin) {
            review = reviewRepository.findById(reviewId)
                    .filter(r -> !r.isDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));
        } else {
            review = reviewRepository.findByIdAndUserId(reviewId, userId)
                    .filter(r -> !r.isDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));
        }
        review.softDelete();
        reviewRepository.save(review);
    }

    @Transactional
    public ReviewResponse moderateReview(Long reviewId, String status) {
        Review review = reviewRepository.findById(reviewId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));

        review.setStatus(Review.Status.valueOf(status.toUpperCase()));
        return ReviewResponse.from(reviewRepository.save(review));
    }
}
