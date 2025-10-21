package entity;

import enums.UserRole;
import enums.UserStatus;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * 用户实体类
 * 
 * 使用EnumSet管理用户角色，支持一个用户拥有多个角色（买家+卖家+管理员）。
 * EnumSet相比多个boolean字段的优势：
 * - 类型安全且高性能（内部使用位向量）
 * - 语义清晰且易扩展（新增角色无需修改类结构）
 * 
 * 信誉分系统：初始100分，范围0-200分，根据交易行为和评价动态调整。
 */
public class User {
    
    private String userId;
    private String username;
    private String password;
    private Set<UserRole> roles;
    private UserStatus status;
    private int reputation;
    private LocalDateTime registerTime;
    private LocalDateTime lastLoginTime;
    
    /**
     * 构造器
     */
    public User(String userId, String username, String password, Set<UserRole> roles) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        // 使用EnumSet.copyOf确保是EnumSet类型
        this.roles = roles.isEmpty() ? EnumSet.noneOf(UserRole.class) : EnumSet.copyOf(roles);
        this.status = UserStatus.ACTIVE;
        this.reputation = 100;  // 初始信誉100分
        this.registerTime = LocalDateTime.now();
        this.lastLoginTime = LocalDateTime.now();
    }
    
    /**
     * 无参构造器（用于JSON反序列化）
     */
    public User() {
        this.roles = EnumSet.noneOf(UserRole.class);
    }
    
    // ========== 角色相关方法 ==========
    
    /**
     * 检查是否拥有指定角色
     */
    public boolean hasRole(UserRole role) {
        return roles.contains(role);
    }
    
    /**
     * 添加角色
     */
    public void addRole(UserRole role) {
        roles.add(role);
    }
    
    /**
     * 移除角色
     */
    public void removeRole(UserRole role) {
        roles.remove(role);
    }
    
    /**
     * 是否是管理员
     */
    public boolean isAdmin() {
        return hasRole(UserRole.ADMIN);
    }
    
    /**
     * 是否是买家
     */
    public boolean isBuyer() {
        return hasRole(UserRole.BUYER);
    }
    
    /**
     * 是否是卖家
     */
    public boolean isSeller() {
        return hasRole(UserRole.SELLER);
    }
    
    // ========== 信誉相关方法 ==========
    
    /**
     * 增加信誉分
     */
    public void increaseReputation(int points) {
        this.reputation = Math.min(this.reputation + points, 200);  // 最高200分
    }
    
    /**
     * 减少信誉分
     */
    public void decreaseReputation(int points) {
        this.reputation = Math.max(this.reputation - points, 0);  // 最低0分
    }
    
    /**
     * 获取信誉等级
     */
    public String getReputationLevel() {
        if (reputation >= 180) return "钻石";
        if (reputation >= 150) return "铂金";
        if (reputation >= 120) return "黄金";
        if (reputation >= 90) return "白银";
        if (reputation >= 60) return "青铜";
        return "警告";
    }
    
    /**
     * 更新最后登录时间
     */
    public void updateLastLoginTime() {
        this.lastLoginTime = LocalDateTime.now();
    }
    
    // ========== Getters and Setters ==========
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public Set<UserRole> getRoles() {
        return roles;
    }
    
    public void setRoles(Set<UserRole> roles) {
        this.roles = roles.isEmpty() ? EnumSet.noneOf(UserRole.class) : EnumSet.copyOf(roles);
    }
    
    public UserStatus getStatus() {
        return status;
    }
    
    public void setStatus(UserStatus status) {
        this.status = status;
    }
    
    public int getReputation() {
        return reputation;
    }
    
    public void setReputation(int reputation) {
        this.reputation = reputation;
    }
    
    public LocalDateTime getRegisterTime() {
        return registerTime;
    }
    
    public void setRegisterTime(LocalDateTime registerTime) {
        this.registerTime = registerTime;
    }
    
    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }
    
    public void setLastLoginTime(LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }
    
    @Override
    public String toString() {
        return String.format("User[id=%s, username=%s, roles=%s, reputation=%d]",
                userId, username, roles, reputation);
    }
}


