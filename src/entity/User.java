package entity;

import enums.UserRole;
import enums.UserStatus;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * 用户实体类
 * 
 * 核心设计：
 * 1. 使用EnumSet<UserRole>管理用户角色（支持多角色）
 * 2. 信誉分系统（默认100分，评价后会变化）
 * 3. 状态管理（正常/封禁/注销）
 * 
 * 答辩要点：
 * 为什么使用EnumSet而不是多个boolean字段？
 * - 类型安全：编译期检查，不会出错
 * - 高性能：内部使用位向量实现
 * - 易扩展：新增角色无需修改类结构
 * - 语义清晰：Set<UserRole>比isBuyer/isSeller更直观
 */
public class User {
    
    private String userId;              // 用户ID（自动生成）
    private String username;            // 用户名
    private String password;            // 密码（实际应该加密）
    private Set<UserRole> roles;        // 角色集合（使用EnumSet）
    private UserStatus status;          // 用户状态
    private int reputation;             // 信誉分（默认100）
    private LocalDateTime registerTime; // 注册时间
    private LocalDateTime lastLoginTime;// 最后登录时间
    
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
        if (reputation >= 180) return "⭐⭐⭐⭐⭐ 钻石";
        if (reputation >= 150) return "⭐⭐⭐⭐ 铂金";
        if (reputation >= 120) return "⭐⭐⭐ 黄金";
        if (reputation >= 90) return "⭐⭐ 白银";
        if (reputation >= 60) return "⭐ 青铜";
        return "⚠ 警告";
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


