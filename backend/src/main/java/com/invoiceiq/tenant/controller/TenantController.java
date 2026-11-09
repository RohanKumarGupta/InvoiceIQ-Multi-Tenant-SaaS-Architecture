package com.invoiceiq.tenant.controller;

import com.invoiceiq.security.UserPrincipal;
import com.invoiceiq.tenant.dto.InviteMemberRequest;
import com.invoiceiq.tenant.dto.MemberResponse;
import com.invoiceiq.tenant.dto.TenantResponse;
import com.invoiceiq.tenant.dto.UpdateRoleRequest;
import com.invoiceiq.tenant.dto.UpdateTenantRequest;
import com.invoiceiq.tenant.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants/current")
@RequiredArgsConstructor
@Tag(name = "Tenant Management")
public class TenantController {

    private final TenantService tenantService;

    @Operation(summary = "Get the current tenant details")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tenant details retrieved"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping
    public ResponseEntity<TenantResponse> getCurrentTenant() {
        UserPrincipal principal = getCurrentUserPrincipal();
        TenantResponse response = tenantService.getTenant(principal.tenantId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update the current tenant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tenant updated successfully"),
            @ApiResponse(responseCode = "403", description = "Only OWNER can update tenant")
    })
    @PreAuthorize("hasRole('OWNER')")
    @PutMapping
    public ResponseEntity<TenantResponse> updateCurrentTenant(
            @Valid @RequestBody UpdateTenantRequest request) {
        UserPrincipal principal = getCurrentUserPrincipal();
        TenantResponse response = tenantService.updateTenant(principal.tenantId(), request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List all members of the current tenant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Members retrieved"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasAnyRole('OWNER', 'ACCOUNTANT')")
    @GetMapping("/members")
    public ResponseEntity<List<MemberResponse>> getMembers() {
        UserPrincipal principal = getCurrentUserPrincipal();
        List<MemberResponse> members = tenantService.getMembers(principal.tenantId());
        return ResponseEntity.ok(members);
    }

    @Operation(summary = "Add a new member to the current tenant")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Member added successfully"),
            @ApiResponse(responseCode = "403", description = "Only OWNER can add members"),
            @ApiResponse(responseCode = "409", description = "Email already in use or invalid role")
    })
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/members")
    public ResponseEntity<MemberResponse> addMember(
            @Valid @RequestBody InviteMemberRequest request) {
        UserPrincipal principal = getCurrentUserPrincipal();
        MemberResponse response = tenantService.addMember(principal.tenantId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Update a member's role")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role updated successfully"),
            @ApiResponse(responseCode = "403", description = "Only OWNER can change roles"),
            @ApiResponse(responseCode = "404", description = "Member not found"),
            @ApiResponse(responseCode = "409", description = "Cannot change OWNER role")
    })
    @PreAuthorize("hasRole('OWNER')")
    @PutMapping("/members/{id}/role")
    public ResponseEntity<MemberResponse> updateMemberRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequest request) {
        UserPrincipal principal = getCurrentUserPrincipal();
        MemberResponse response = tenantService.updateMemberRole(principal.tenantId(), id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Remove a member from the current tenant (soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Member removed successfully"),
            @ApiResponse(responseCode = "403", description = "Only OWNER can remove members"),
            @ApiResponse(responseCode = "404", description = "Member not found"),
            @ApiResponse(responseCode = "409", description = "Cannot remove OWNER")
    })
    @PreAuthorize("hasRole('OWNER')")
    @DeleteMapping("/members/{id}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID id) {
        UserPrincipal principal = getCurrentUserPrincipal();
        tenantService.removeMember(principal.tenantId(), id);
        return ResponseEntity.noContent().build();
    }

    private UserPrincipal getCurrentUserPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }
}
