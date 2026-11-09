package com.invoiceiq.tenant.dto;

import com.invoiceiq.auth.model.Role;

import java.time.Instant;
import java.util.UUID;

public record MemberResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        Role role,
        boolean active,
        Instant createdAt
) {}
