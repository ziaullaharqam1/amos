package com.amos.ams.service;

import com.amos.ams.audit.AuditService;
import com.amos.ams.domain.Permission;
import com.amos.ams.domain.Role;
import com.amos.ams.domain.User;
import com.amos.ams.dto.Dtos;
import com.amos.ams.exception.ApiException;
import com.amos.ams.repository.PermissionRepository;
import com.amos.ams.repository.RoleRepository;
import com.amos.ams.repository.UserRepository;
import com.amos.ams.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class IdentityService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final PermissionRepository permissions;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final AuditService audit;

    public IdentityService(UserRepository users, RoleRepository roles, PermissionRepository permissions,
                           PasswordEncoder encoder, AuthenticationManager authManager, JwtService jwtService,
                           AuditService audit) {
        this.users = users;
        this.roles = roles;
        this.permissions = permissions;
        this.encoder = encoder;
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.audit = audit;
    }

    public Dtos.AuthResponse login(Dtos.LoginRequest request) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        User user = users.findByUsername(request.username()).orElseThrow();
        return new Dtos.AuthResponse(jwtService.generate(user.getUsername()), toView(user));
    }

    public Dtos.UserView me(User user) {
        return toView(user);
    }

    public List<Dtos.UserView> listUsers() {
        return users.findAll().stream().map(this::toView).toList();
    }

    @Transactional
    public Dtos.UserView createUser(Dtos.UserUpsert req, User actor) {
        if (users.existsByUsername(req.username())) throw ApiException.conflict("Username already exists");
        if (users.existsByEmail(req.email())) throw ApiException.conflict("Email already exists");
        if (req.password() == null || req.password().length() < 8) {
            throw ApiException.badRequest("Password must be at least 8 characters");
        }
        User user = new User();
        apply(user, req, true);
        users.save(user);
        audit.record(actor, "CREATE", "User", user.getId(), null, toView(user));
        return toView(user);
    }

    @Transactional
    public Dtos.UserView updateUser(Long id, Dtos.UserUpsert req, User actor) {
        User user = users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
        Dtos.UserView before = toView(user);
        apply(user, req, false);
        users.save(user);
        audit.record(actor, "UPDATE", "User", id, before, toView(user));
        return toView(user);
    }

    public List<Dtos.RoleView> listRoles() {
        return roles.findAll().stream().map(this::toRoleView).toList();
    }

    public List<Dtos.PermissionView> listPermissions() {
        return permissions.findAll().stream()
                .map(p -> new Dtos.PermissionView(p.getId(), p.getCode(), p.getName(), p.getResource(), p.getDescription()))
                .toList();
    }

    @Transactional
    public Dtos.RoleView createRole(Dtos.RoleUpsert req, User actor) {
        Role role = new Role();
        role.setCode(req.code().toUpperCase());
        role.setName(req.name());
        role.setDescription(req.description());
        role.setPermissions(loadPermissions(req.permissionIds()));
        roles.save(role);
        audit.record(actor, "CREATE", "Role", role.getId(), null, toRoleView(role));
        return toRoleView(role);
    }

    @Transactional
    public Dtos.RoleView updateRole(Long id, Dtos.RoleUpsert req, User actor) {
        Role role = roles.findById(id).orElseThrow(() -> ApiException.notFound("Role not found"));
        Dtos.RoleView before = toRoleView(role);
        if (!role.isSystemRole()) {
            role.setCode(req.code().toUpperCase());
            role.setName(req.name());
        }
        role.setDescription(req.description());
        role.setPermissions(loadPermissions(req.permissionIds()));
        roles.save(role);
        audit.record(actor, "UPDATE", "Role", id, before, toRoleView(role));
        return toRoleView(role);
    }

    private void apply(User user, Dtos.UserUpsert req, boolean creating) {
        if (creating) user.setUsername(req.username());
        user.setEmail(req.email());
        user.setFullName(req.fullName());
        user.setPhone(req.phone());
        user.setStation(req.station());
        user.setActive(req.active());
        if (req.password() != null && !req.password().isBlank()) {
            user.setPasswordHash(encoder.encode(req.password()));
        }
        if (req.roleIds() != null) {
            Set<Role> assigned = new HashSet<>(roles.findAllById(req.roleIds()));
            user.setRoles(assigned);
        }
    }

    private Set<Permission> loadPermissions(List<Long> ids) {
        if (ids == null) return new HashSet<>();
        return new HashSet<>(permissions.findAllById(ids));
    }

    public Dtos.UserView toView(User user) {
        Set<String> roleCodes = user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
        Set<String> perms = user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(Permission::getCode)
                .collect(Collectors.toSet());
        return new Dtos.UserView(user.getId(), user.getUsername(), user.getEmail(), user.getFullName(),
                user.getPhone(), user.getStation(), user.isActive(), roleCodes, perms);
    }

    private Dtos.RoleView toRoleView(Role role) {
        return new Dtos.RoleView(role.getId(), role.getCode(), role.getName(), role.getDescription(), role.isSystemRole(),
                role.getPermissions().stream().map(Permission::getCode).collect(Collectors.toSet()));
    }
}
