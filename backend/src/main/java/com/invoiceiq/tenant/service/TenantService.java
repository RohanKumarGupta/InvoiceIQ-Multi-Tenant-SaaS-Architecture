package com.invoiceiq.tenant.service;

import com.invoiceiq.auth.model.Role;
import com.invoiceiq.auth.model.User;
import com.invoiceiq.auth.repository.UserRepository;
import com.invoiceiq.common.exception.ConflictException;
import com.invoiceiq.common.exception.ResourceNotFoundException;
import com.invoiceiq.tenant.dto.InviteMemberRequest;
import com.invoiceiq.tenant.dto.MemberResponse;
import com.invoiceiq.tenant.dto.TenantResponse;
import com.invoiceiq.tenant.dto.UpdateTenantRequest;
import com.invoiceiq.tenant.dto.UpdateRoleRequest;
import com.invoiceiq.tenant.model.Tenant;
import com.invoiceiq.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TenantResponse getTenant(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        return toTenantResponse(tenant);
    }

    @Transactional
    public TenantResponse updateTenant(UUID tenantId, UpdateTenantRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        if (request.name() != null && !request.name().isBlank()) {
            tenant.setName(request.name());
        }

        if (request.settings() != null) {
            Map<String, Object> merged = tenant.getSettings() != null
                    ? new HashMap<>(tenant.getSettings())
                    : new HashMap<>();
            merged.putAll(request.settings());
            tenant.setSettings(merged);
        }

        tenant = tenantRepository.save(tenant);
        return toTenantResponse(tenant);
    }

    public List<MemberResponse> getMembers(UUID tenantId) {
        return userRepository.findAllByTenantId(tenantId).stream()
                .map(this::toMemberResponse)
                .toList();
    }

    @Transactional
    public MemberResponse addMember(UUID tenantId, InviteMemberRequest request) {
        if (userRepository.existsByEmailAndTenantId(request.email(), tenantId)) {
            throw new ConflictException("A user with this email already exists in this tenant");
        }

        if (request.role() == Role.OWNER) {
            throw new ConflictException("Cannot add another OWNER to the tenant");
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .role(request.role())
                .active(true)
                .emailVerified(false)
                .build();
        user.setTenantId(tenantId);
        user = userRepository.save(user);

        return toMemberResponse(user);
    }

    @Transactional
    public MemberResponse updateMemberRole(UUID tenantId, UUID memberId, UpdateRoleRequest request) {
        User user = userRepository.findByIdAndTenantId(memberId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        if (user.getRole() == Role.OWNER) {
            throw new ConflictException("Cannot change the role of the OWNER");
        }

        if (request.role() == Role.OWNER) {
            throw new ConflictException("Cannot assign OWNER role to a member");
        }

        user.setRole(request.role());
        user = userRepository.save(user);

        return toMemberResponse(user);
    }

    @Transactional
    public void removeMember(UUID tenantId, UUID memberId) {
        User user = userRepository.findByIdAndTenantId(memberId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        if (user.getRole() == Role.OWNER) {
            throw new ConflictException("Cannot remove the OWNER from the tenant");
        }

        user.setActive(false);
        userRepository.save(user);
    }

    // ── Private helpers ──

    private TenantResponse toTenantResponse(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getSlug(),
                tenant.getPlan(),
                tenant.getSettings(),
                tenant.getCreatedAt()
        );
    }

    private MemberResponse toMemberResponse(User user) {
        return new MemberResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
