package service;

import entity.User;
import enums.UserRole;
import enums.UserStatus;
import exception.AuthenticationException;
import exception.BusinessException;
import exception.ResourceNotFoundException;
import observer.UserMessageReceiver;
import repository.DataCenter;
import util.IdGenerator;
import util.InputValidator;
import util.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

/**
 * 用户服务
 * 
 * 核心功能：
 * 1. 用户注册、登录、登出
 * 2. 用户信息管理
 * 3. 权限校验
 * 4. 观察者生命周期管理（登录时订阅，登出时退订）
 * 
 * 答辩要点：
 * - 观察者生命周期管理：登录时创建MessageReceiver并订阅，登出时退订
 * - 密码加密存储：使用SHA-256哈希
 * - 输入验证：用户名、密码格式验证
 */
public class UserService {
    
    private final DataCenter dataCenter;
    private final NotificationService notificationService;
    
    /**
     * 当前登录用户
     */
    private User currentUser;
    
    /**
     * 当前用户的消息接收器
     */
    private UserMessageReceiver currentReceiver;
    
    /**
     * 构造器（依赖注入）
     */
    public UserService(NotificationService notificationService) {
        this.dataCenter = DataCenter.getInstance();
        this.notificationService = notificationService;
    }
    
    // ========== 注册与登录 ==========
    
    /**
     * 用户注册
     */
    public User register(String username, String password, Set<UserRole> roles) {
        // 1. 验证输入
        if (!InputValidator.isValidUsername(username)) {
            throw new BusinessException("用户名格式不正确（4-20位字母数字）");
        }
        if (!InputValidator.isValidPassword(password)) {
            throw new BusinessException("密码格式不正确（6-20位）");
        }
        if (roles.isEmpty()) {
            throw new BusinessException("至少选择一个角色");
        }
        
        // 2. 检查用户名是否存在
        if (dataCenter.existsUsername(username)) {
            throw new BusinessException("用户名已存在");
        }
        
        // 3. 创建用户（密码加密）
        String userId = IdGenerator.generateUserId();
        String encodedPassword = PasswordEncoder.encode(password);
        User user = new User(userId, username, encodedPassword, roles);
        
        // 4. 保存到数据中心
        dataCenter.addUser(user);
        
        return user;
    }
    
    /**
     * 用户登录
     */
    public User login(String username, String password) {
        // 1. 查找用户
        User user = dataCenter.findUserByUsername(username)
                .orElseThrow(() -> new AuthenticationException("用户名或密码错误"));
        
        // 2. 验证密码
        if (!PasswordEncoder.matches(password, user.getPassword())) {
            throw new AuthenticationException("用户名或密码错误");
        }
        
        // 3. 检查用户状态
        if (user.getStatus() == UserStatus.BANNED) {
            throw new AuthenticationException("用户已被封禁");
        }
        if (user.getStatus() == UserStatus.DELETED) {
            throw new AuthenticationException("用户已注销");
        }
        
        // 4. 更新登录时间
        user.updateLastLoginTime();
        
        // 5. 设置当前用户
        this.currentUser = user;
        
        // 6. 创建消息接收器并订阅通知（观察者模式）
        this.currentReceiver = new UserMessageReceiver(user.getUserId());
        this.notificationService.subscribe(user.getUserId(), currentReceiver);
        
        // 7. 发送欢迎消息
        notificationService.notify(user.getUserId(), "欢迎回来，" + user.getUsername() + "！");
        
        return user;
    }
    
    /**
     * 用户登出
     */
    public void logout() {
        if (currentUser != null && currentReceiver != null) {
            // 退订通知
            notificationService.unsubscribe(currentUser.getUserId(), currentReceiver);
            currentUser = null;
            currentReceiver = null;
        }
    }
    
    // ========== 状态查询 ==========
    
    /**
     * 是否已登录
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }
    
    /**
     * 获取当前用户
     */
    public User getCurrentUser() {
        if (currentUser == null) {
            throw new AuthenticationException("未登录");
        }
        return currentUser;
    }
    
    /**
     * 获取当前用户的消息接收器
     */
    public UserMessageReceiver getCurrentReceiver() {
        if (currentReceiver == null) {
            throw new AuthenticationException("未登录");
        }
        return currentReceiver;
    }
    
    /**
     * 检查当前用户是否有指定角色
     */
    public boolean hasRole(UserRole role) {
        return isLoggedIn() && currentUser.hasRole(role);
    }
    
    /**
     * 要求当前用户必须有指定角色
     */
    public void requireRole(UserRole role) {
        if (!hasRole(role)) {
            throw new exception.PermissionDeniedException(
                "需要" + role.getDisplayName() + "角色");
        }
    }
    
    // ========== 用户信息管理 ==========
    
    /**
     * 根据ID获取用户
     */
    public User getUserById(String userId) {
        return dataCenter.findUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
    }
    
    /**
     * 修改密码
     */
    public void changePassword(String oldPassword, String newPassword) {
        User user = getCurrentUser();
        
        // 验证旧密码
        if (!PasswordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        
        // 验证新密码
        if (!InputValidator.isValidPassword(newPassword)) {
            throw new BusinessException("新密码格式不正确（6-20位）");
        }
        
        // 更新密码
        user.setPassword(PasswordEncoder.encode(newPassword));
    }
    
    /**
     * 更新用户信誉
     */
    public void updateReputation(String userId, int change) {
        User user = getUserById(userId);
        if (change > 0) {
            user.increaseReputation(change);
        } else if (change < 0) {
            user.decreaseReputation(-change);
        }
    }
    
    /**
     * 获取所有卖家
     */
    public java.util.List<User> getAllSellers() {
        return dataCenter.getAllUsers().stream()
                .filter(u -> u.hasRole(UserRole.SELLER))
                .toList();
    }
}


