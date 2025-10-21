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
import util.SimpleLogger;
import util.ValidationUtils;

import java.util.Optional;
import java.util.Set;

/**
 * 用户服务
 * 
 * 负责用户认证、会话管理、权限校验和用户信息维护。
 * 采用观察者模式管理消息订阅：登录时自动订阅，登出时自动退订。
 * 所有密码使用SHA-256加密后存储。
 */
public class UserService {
    
    private static final SimpleLogger logger = SimpleLogger.getLogger(UserService.class);
    
    private final DataCenter dataCenter;
    private final NotificationService notificationService;
    private User currentUser;
    private UserMessageReceiver currentReceiver;
    
    /**
     * 构造用户服务
     * 
     * @param notificationService 通知服务，用于消息订阅管理
     */
    public UserService(NotificationService notificationService) {
        this.dataCenter = DataCenter.getInstance();
        this.notificationService = notificationService;
    }
    
    /**
     * 注册新用户
     * 
     * @param username 用户名，必须4-20位字母或数字
     * @param password 密码，必须6-20位字符
     * @param roles 用户角色集合，至少包含一个角色
     * @return 创建的用户对象
     * @throws BusinessException 如果用户名已存在或输入验证失败
     */
    public User register(String username, String password, Set<UserRole> roles) {
        logger.info("用户注册请求: username={}, roles={}", username, roles);
        
        try {
            ValidationUtils.validateUsername(username);
            ValidationUtils.validatePassword(password);
            
            if (roles.isEmpty()) {
                throw new BusinessException("至少需要选择一个角色");
            }
            
            if (dataCenter.existsUsername(username)) {
                throw new BusinessException("用户名已存在");
            }
            
            String userId = IdGenerator.generateUserId();
            String encodedPassword = PasswordEncoder.encode(password);
            User user = new User(userId, username, encodedPassword, roles);
            
            dataCenter.addUser(user);
            
            logger.info("注册成功: userId={}, username={}", userId, username);
            return user;
            
        } catch (BusinessException e) {
            logger.error("注册失败: username={}, error={}", username, e.getMessage());
            throw e;
        }
    }
    
    /**
     * 用户登录
     * 
     * 验证凭据后建立会话，并自动订阅消息通知。
     * 被封禁的用户可以登录但功能受限。
     * 
     * @param username 用户名
     * @param password 明文密码
     * @return 登录的用户对象
     * @throws AuthenticationException 如果凭据错误或账号已注销
     */
    public User login(String username, String password) {
        logger.info("登录请求: username={}", username);
        
        try {
            User user = findAndValidateUser(username, password);
            validateAccountStatus(user);
            
            user.updateLastLoginTime();
            establishSession(user);
            sendWelcomeMessage(user);
            
            logger.info("登录成功: userId={}, username={}, status={}", 
                       user.getUserId(), username, user.getStatus());
            
            return user;
            
        } catch (AuthenticationException e) {
            logger.error("登录失败: username={}, error={}", username, e.getMessage());
            throw e;
        }
    }
    
    /**
     * 用户登出
     * 
     * 清理会话并退订消息通知。
     */
    public void logout() {
        if (currentUser != null && currentReceiver != null) {
            logger.info("登出: userId={}, username={}", 
                       currentUser.getUserId(), currentUser.getUsername());
            
            notificationService.unsubscribe(currentUser.getUserId(), currentReceiver);
            currentUser = null;
            currentReceiver = null;
        }
    }
    
    /**
     * 检查是否已登录
     * 
     * @return 如果有用户登录返回true
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }
    
    /**
     * 获取当前登录用户
     * 
     * @return 当前用户对象
     * @throws AuthenticationException 如果未登录
     */
    public User getCurrentUser() {
        if (currentUser == null) {
            throw new AuthenticationException("未登录");
        }
        return currentUser;
    }
    
    /**
     * 获取当前用户的消息接收器
     * 
     * @return 消息接收器
     * @throws AuthenticationException 如果未登录
     */
    public UserMessageReceiver getCurrentReceiver() {
        if (currentReceiver == null) {
            throw new AuthenticationException("未登录");
        }
        return currentReceiver;
    }
    
    /**
     * 检查当前用户是否拥有指定角色
     * 
     * @param role 要检查的角色
     * @return 如果用户已登录且拥有该角色返回true
     */
    public boolean hasRole(UserRole role) {
        return isLoggedIn() && currentUser.hasRole(role);
    }
    
    /**
     * 要求当前用户必须拥有指定角色
     * 
     * @param role 必需的角色
     * @throws exception.PermissionDeniedException 如果用户未登录或没有该角色
     */
    public void requireRole(UserRole role) {
        if (!hasRole(role)) {
            throw new exception.PermissionDeniedException(
                "需要" + role.getDisplayName() + "角色");
        }
    }
    
    /**
     * 根据ID获取用户
     * 
     * @param userId 用户ID
     * @return 用户对象
     * @throws ResourceNotFoundException 如果用户不存在
     */
    public User getUserById(String userId) {
        return dataCenter.findUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
    }
    
    /**
     * 修改当前用户密码
     * 
     * @param oldPassword 旧密码
     * @param newPassword 新密码，必须6-20位字符
     * @throws BusinessException 如果旧密码错误或新密码格式无效
     */
    public void changePassword(String oldPassword, String newPassword) {
        User user = getCurrentUser();
        logger.info("修改密码请求: userId={}", user.getUserId());
        
        try {
            if (!PasswordEncoder.matches(oldPassword, user.getPassword())) {
                throw new BusinessException("旧密码错误");
            }
            
            ValidationUtils.validatePassword(newPassword);
            user.setPassword(PasswordEncoder.encode(newPassword));
            
            logger.info("修改密码成功: userId={}", user.getUserId());
            
        } catch (BusinessException e) {
            logger.error("修改密码失败: userId={}, error={}", 
                        user.getUserId(), e.getMessage());
            throw e;
        }
    }
    
    /**
     * 更新用户信誉分
     * 
     * @param userId 用户ID
     * @param change 信誉变化量，正数增加，负数减少
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
     * 获取所有卖家用户
     * 
     * @return 拥有卖家角色的用户列表
     */
    public java.util.List<User> getAllSellers() {
        return dataCenter.getAllUsers().stream()
                .filter(u -> u.hasRole(UserRole.SELLER))
                .toList();
    }
    
    /**
     * 查找并验证用户凭据
     * 
     * 为避免泄露用户存在性，用户不存在和密码错误返回相同的错误信息。
     */
    private User findAndValidateUser(String username, String password) {
        User user = dataCenter.findUserByUsername(username)
                .orElseThrow(() -> new AuthenticationException("用户名或密码错误"));
        
        if (!PasswordEncoder.matches(password, user.getPassword())) {
            throw new AuthenticationException("用户名或密码错误");
        }
        
        return user;
    }
    
    /**
     * 验证账号状态
     * 
     * 已注销的账号无法登录，被封禁的账号可以登录但功能受限。
     */
    private void validateAccountStatus(User user) {
        if (user.getStatus() == UserStatus.DELETED) {
            throw new AuthenticationException("账号已注销");
        }
    }
    
    /**
     * 建立用户会话并订阅消息
     * 
     * 使用观察者模式，登录时自动为用户创建消息接收器并订阅通知服务。
     */
    private void establishSession(User user) {
        this.currentUser = user;
        this.currentReceiver = new UserMessageReceiver(user.getUserId());
        this.notificationService.subscribe(user.getUserId(), currentReceiver);
    }
    
    /**
     * 发送欢迎消息
     */
    private void sendWelcomeMessage(User user) {
        notificationService.notify(user.getUserId(), "欢迎回来，" + user.getUsername() + "！");
    }
}


