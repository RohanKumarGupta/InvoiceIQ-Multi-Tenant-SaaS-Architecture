package com.invoiceiq.tenant.dto;

import com.invoiceiq.auth.model.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(
        @NotNull Role role
) {}
