package com.user.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    private UUID id;
    private String email;
    private String phone;
    private String firstName;
    private String lastName;
    private Boolean isActive=true;
    private String locale="en";
    private OffsetDateTime emailVerifiedAt;
    private OffsetDateTime phoneVerifiedAt;
    private String metadata;
    private OffsetDateTime createdAt= OffsetDateTime.now();
    private OffsetDateTime updatedAt=OffsetDateTime.now();
}
