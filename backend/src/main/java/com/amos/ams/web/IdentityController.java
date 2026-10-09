package com.amos.ams.web;

import com.amos.ams.dto.Dtos;
import com.amos.ams.security.CurrentUser;
import com.amos.ams.service.IdentityService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class IdentityController {
    private final IdentityService identity;
    private final CurrentUser currentUser;

    public IdentityController(IdentityService identity, CurrentUser currentUser) {
        this.identity = identity;
        this.currentUser = currentUser;
    }

    @PostMapping("/auth/login")
    public Dtos.AuthResponse login(@Valid @RequestBody Dtos.LoginRequest request) {
        return identity.login(request);
    }

    @GetMapping("/me")
    public Dtos.UserView me() {
        return identity.me(currentUser.require());
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public List<Dtos.UserView> users() {
        return identity.listUsers();
    }

    @GetMapping("/directory")
    @PreAuthorize("hasAnyAuthority('ACTIVITY_ASSIGN','USER_MANAGE')")
    public List<Dtos.UserView> directory() {
        return identity.listUsers();
    }

    @PostMapping("/users")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public Dtos.UserView createUser(@Valid @RequestBody Dtos.UserUpsert request) {
        return identity.createUser(request, currentUser.require());
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public Dtos.UserView updateUser(@PathVariable Long id, @Valid @RequestBody Dtos.UserUpsert request) {
        return identity.updateUser(id, request, currentUser.require());
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAnyAuthority('USER_MANAGE','ROLE_MANAGE')")
    public List<Dtos.RoleView> roles() {
        return identity.listRoles();
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public Dtos.RoleView createRole(@Valid @RequestBody Dtos.RoleUpsert request) {
        return identity.createRole(request, currentUser.require());
    }

    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public Dtos.RoleView updateRole(@PathVariable Long id, @Valid @RequestBody Dtos.RoleUpsert request) {
        return identity.updateRole(id, request, currentUser.require());
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public List<Dtos.PermissionView> permissions() {
        return identity.listPermissions();
    }
}
