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
 * 负责用户管理和系统统计功能。
 * 所有操作都需要管理员权限。
 * 封禁用户会扣除50点信誉分，解封恢复20点。
 */
public class AdminService {
    
    private static final int REPUTATION_PENALTY_BAN = 50;
    private static final int REPUTATION_REWARD_UNBAN = 20;
    
    private final DataCenter dataCenter;
    private final UserService userService;
    
    public AdminService(UserService userService) {
        this.dataCenter = DataCenter.getInstance();
        this.userService = userService;
    }
    
    /**
     * 封禁用户
     * 
     * 封禁后用户状态变为BANNED，扣除50点信誉分。
     * 
     * @param admin 管理员
     * @param userId 要封禁的用户ID
     * @throws PermissionDeniedException 如果不是管理员
     * @throws ResourceNotFoundException 如果用户不存在
     */
    public void banUser(User admin, String userId) {
        requireAdmin(admin);
        
        User user = dataCenter.findUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        
        user.setStatus(UserStatus.BANNED);
        user.decreaseReputation(REPUTATION_PENALTY_BAN);
    }
    
    /**
     * 解封用户
     * 
     * 解封后用户状态恢复为ACTIVE，增加20点信誉分。
     * 
     * @param admin 管理员
     * @param userId 要解封的用户ID
     * @throws PermissionDeniedException 如果不是管理员
     * @throws ResourceNotFoundException 如果用户不存在
     */
    public void unbanUser(User admin, String userId) {
        requireAdmin(admin);
        
        User user = dataCenter.findUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        
        user.setStatus(UserStatus.ACTIVE);
        user.increaseReputation(REPUTATION_REWARD_UNBAN);
    }
    
    /**
     * 获取所有用户
     * 
     * @param admin 管理员
     * @return 用户列表
     * @throws PermissionDeniedException 如果不是管理员
     */
    public List<User> getAllUsers(User admin) {
        requireAdmin(admin);
        return dataCenter.getAllUsers();
    }
    
    /**
     * 获取系统统计信息
     * 
     * @param admin 管理员
     * @return 系统统计数据
     * @throws PermissionDeniedException 如果不是管理员
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
     * 验证管理员权限
     */
    private void requireAdmin(User user) {
        if (!user.hasRole(UserRole.ADMIN)) {
            throw new PermissionDeniedException("需要管理员权限");
        }
    }
    
    /**
     * 系统统计数据
     * 
     * @param userCount 用户总数
     * @param productCount 商品总数
     * @param orderCount 订单总数
     * @param reviewCount 评价总数
     */
    public record SystemStats(
        int userCount,
        int productCount,
        int orderCount,
        int reviewCount
    ) {}
}


