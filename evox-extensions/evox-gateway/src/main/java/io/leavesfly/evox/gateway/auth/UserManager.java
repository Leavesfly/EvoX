package io.leavesfly.evox.gateway.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户管理器
 * 提供用户 CRUD 和角色管理能力
 */
@Slf4j
public class UserManager {

    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_USER = "user";
    public static final String ROLE_VIEWER = "viewer";

    private final Map<String, UserProfile> users = new ConcurrentHashMap<>();

    private final Map<String, Set<String>> rolePermissions = new HashMap<>();

    public UserManager() {
        initRolePermissions();
    }

    private void initRolePermissions() {
        rolePermissions.put(ROLE_ADMIN, Set.of("*"));
        rolePermissions.put(ROLE_USER, Set.of("chat", "tools", "skills"));
        rolePermissions.put(ROLE_VIEWER, Set.of("chat"));
    }

    public UserProfile createUser(String userId, String username, String email, Set<String> roles) {
        if (userId == null || username == null) {
            throw new IllegalArgumentException("userId and username cannot be null");
        }

        if (users.containsKey(userId)) {
            throw new IllegalArgumentException("User already exists: " + userId);
        }

        Set<String> effectiveRoles = roles != null ? roles : Set.of();
        Set<String> permissions = getPermissionsForRoles(effectiveRoles);

        UserProfile profile = UserProfile.builder()
                .userId(userId)
                .username(username)
                .email(email)
                .roles(new HashSet<>(effectiveRoles))
                .permissions(permissions)
                .enabled(true)
                .createdAt(Instant.now())
                .metadata(new HashMap<>())
                .build();

        users.put(userId, profile);
        log.info("Created user: {} with roles: {}", userId, effectiveRoles);
        return profile;
    }

    public Optional<UserProfile> getUser(String userId) {
        return Optional.ofNullable(users.get(userId));
    }

    public UserProfile updateUser(String userId, String username, String email) {
        UserProfile profile = users.get(userId);
        if (profile == null) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        if (username != null) {
            profile.setUsername(username);
        }
        if (email != null) {
            profile.setEmail(email);
        }

        log.info("Updated user: {}", userId);
        return profile;
    }

    public boolean deleteUser(String userId) {
        UserProfile removed = users.remove(userId);
        if (removed != null) {
            log.info("Deleted user: {}", userId);
            return true;
        }
        return false;
    }

    public List<UserProfile> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    public int getUserCount() {
        return users.size();
    }

    public boolean assignRole(String userId, String role) {
        UserProfile profile = users.get(userId);
        if (profile == null) {
            log.warn("Cannot assign role to non-existent user: {}", userId);
            return false;
        }

        if (profile.getRoles().add(role)) {
            profile.setPermissions(getPermissionsForRoles(profile.getRoles()));
            log.info("Assigned role {} to user {}", role, userId);
            return true;
        }
        return false;
    }

    public boolean removeRole(String userId, String role) {
        UserProfile profile = users.get(userId);
        if (profile == null) {
            log.warn("Cannot remove role from non-existent user: {}", userId);
            return false;
        }

        if (profile.getRoles().remove(role)) {
            profile.setPermissions(getPermissionsForRoles(profile.getRoles()));
            log.info("Removed role {} from user {}", role, userId);
            return true;
        }
        return false;
    }

    public Set<String> getPermissionsForRoles(Set<String> roles) {
        Set<String> permissions = new HashSet<>();
        for (String role : roles) {
            Set<String> rolePerms = rolePermissions.get(role);
            if (rolePerms != null) {
                permissions.addAll(rolePerms);
            }
        }
        return permissions;
    }

    public Optional<UserSession> createSessionForUser(String userId) {
        UserProfile profile = users.get(userId);
        if (profile == null) {
            log.warn("Cannot create session for non-existent user: {}", userId);
            return Optional.empty();
        }

        if (!profile.isEnabled()) {
            log.warn("Cannot create session for disabled user: {}", userId);
            return Optional.empty();
        }

        UserSession session = UserSession.builder()
                .userId(profile.getUserId())
                .username(profile.getUsername())
                .permissions(profile.getPermissions())
                .build();

        log.info("Created session for user: {}", userId);
        return Optional.of(session);
    }

    public void initDefaultAdmin() {
        if (!users.containsKey("admin")) {
            createUser("admin", "Administrator", null, Set.of(ROLE_ADMIN));
            log.info("Initialized default admin user");
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserProfile {
        private String userId;
        private String username;
        private String email;
        private Set<String> roles;
        private Set<String> permissions;
        private boolean enabled;
        private Instant createdAt;
        private Map<String, Object> metadata;
    }
}
