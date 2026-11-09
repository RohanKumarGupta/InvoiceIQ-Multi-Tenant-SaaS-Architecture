package com.invoiceiq.security;

import com.invoiceiq.auth.model.Role;

import java.util.UUID;

public record UserPrincipal(
    UUID userId,
    UUID tenantId,
    String email,
    Role role
) {}
