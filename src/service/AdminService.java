package service;

import entity.User;
import enums.UserRole;
import enums.UserStatus;
import exception.PermissionDeniedException;
import exception.ResourceNotFoundException;
import repository.DataCenter;

import java.util.List;

/**
 * 管理员服务
 * 
 * 核心功能：
 * 1. 用户管理（封禁、解封）
 * 2. 系统统计
 * 
 * 答辩要点：
 * - 权限校验：所有操作都需要管理员权限
 * - 统计功能：展示系统运行数据
 */
public class AdminService {
    
    private final DataCenter dataCenter;
    private final UserService userService;
    
    public AdminService(UserService userService) {
        this.dataCenter = DataCenter.getInstance();
        this.userService = userService;
    }
    
    // ========== 权限校验 ==========
    
    /**
     * 要求管理员权限
     */
    private void requireAdmin(User user) {
        if (!user.hasRole(UserRole.ADMIN)) {
            throw new PermissionDeniedException("需要管理员权限");
        }
    }
    
    // ========== 用户管理 ==========
    
    /**
     * 封禁用户
     */
    public void banUser(User admin, String userId) {
        requireAdmin(admin);
        
        User user = dataCenter.findUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        
        // 封禁并扣除信誉分
        user.setStatus(UserStatus.BANNED);
        user.decreaseReputation(50);
    }
    
    /**
     * 解封用户
     */
    public void unbanUser(User admin, String userId) {
        requireAdmin(admin);
        
        User user = dataCenter.findUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        
        // 解封并恢复部分信誉分
        user.setStatus(UserStatus.ACTIVE);
        user.increaseReputation(20);
    }
    
    /**
     * 获取所有用户
     */
    public List<User> getAllUsers(User admin) {
        requireAdmin(admin);
        return dataCenter.getAllUsers();
    }
    
    // ========== 系统统计 ==========
    
    /**
     * 获取系统统计信息
     */
    public SystemStats getSystemStats(User admin) {
        requireAdmin(admin);
        
        return new SystemStats(
            dataCenter.getUserCount(),
            dataCenter.getProductCount(),
            dataCenter.getOrderCount(),
            dataCenter.getReviewCount()
        );
    }
    
    /**
     * 系统统计数据类
     */
    public record SystemStats(
        int userCount,
        int productCount,
        int orderCount,
        int reviewCount
    ) {}
}


