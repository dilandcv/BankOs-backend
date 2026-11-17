package com.bankos.modules.users.mapper;

import com.bankos.modules.users.dto.UserResponse;
import com.bankos.modules.users.dto.UserSummary;
import com.bankos.modules.users.entity.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getStatus().name(),
                user.getCreatedAt()
        );
    }

    public static UserSummary toSummary(User user) {
        return new UserSummary(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole()
        );
    }
}
