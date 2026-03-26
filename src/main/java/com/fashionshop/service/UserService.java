package com.fashionshop.service;

import com.fashionshop.dto.request.UpdateProfileRequest;
import com.fashionshop.dto.request.UpdateUserRequest;
import com.fashionshop.dto.response.UserResponse;
import com.fashionshop.entity.User;
import com.fashionshop.exception.BadRequestException;
import com.fashionshop.exception.ResourceNotFoundException;
import com.fashionshop.repository.UserRepository;
import com.fashionshop.repository.UserSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "name", "email");

    public Page<UserResponse> getUsers(String search, String status, String sort,
                                        int page, int size) {
        size = Math.min(size, 50);
        String sortField = ALLOWED_SORT_FIELDS.contains(sort) ? sort : "createdAt";
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortField));

        return userRepository
                .findAll(UserSpecification.withFilters(search, status), pageable)
                .map(UserResponse::from);
    }

    public UserResponse getUserById(Long id) {
        User user = findActiveUser(id);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest req) {
        User user = findActiveUser(id);

        if (req.getName() != null) user.setName(req.getName());
        if (req.getAvatar() != null) user.setAvatar(req.getAvatar());
        if (req.getRole() != null) {
            user.setRole(User.Role.valueOf(req.getRole()));
        }

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id, Long currentUserId) {
        if (id.equals(currentUserId)) {
            throw new BadRequestException("Cannot delete your own account");
        }
        User user = findActiveUser(id);
        user.softDelete();
        userRepository.save(user);
    }

    @Transactional
    public UserResponse toggleStatus(Long id, Long currentUserId) {
        if (id.equals(currentUserId)) {
            throw new BadRequestException("Cannot change your own status");
        }
        User user = findActiveUser(id);
        user.setStatus(user.getStatus() == User.Status.ACTIVE
                ? User.Status.INACTIVE
                : User.Status.ACTIVE);
        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse getMyProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateMyProfile(String email, UpdateProfileRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (req.getName() != null) user.setName(req.getName());
        if (req.getAvatar() != null) user.setAvatar(req.getAvatar());

        return UserResponse.from(userRepository.save(user));
    }

    private User findActiveUser(Long id) {
        return userRepository.findById(id)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
