package com.user.utility;
import com.user.dto.UserDto;
import com.user.model.User;

import java.time.OffsetDateTime;

public class UserMapper {

    public static UserDto toDto(User user) {
        if (user == null) return null;

        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .isActive(user.getIsActive())
                .locale(user.getLocale())
                .emailVerifiedAt(user.getEmailVerifiedAt())
                .phoneVerifiedAt(user.getPhoneVerifiedAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public static User fromDto(UserDto userdto) {
        if (userdto == null) return null;

        return User.builder()
                .id(userdto.getId())
                .email(userdto.getEmail())
                .phone(userdto.getPhone())
                .firstName(userdto.getFirstName())
                .lastName(userdto.getLastName())
                .isActive(userdto.getIsActive())
                .locale(userdto.getLocale())
                .emailVerifiedAt(userdto.getEmailVerifiedAt())
                .phoneVerifiedAt(userdto.getPhoneVerifiedAt())
                .createdAt(userdto.getCreatedAt())
                .updatedAt(userdto.getUpdatedAt())
                .build();
    }
}
