package factory;

import entity.User;
import enums.UserRole;
import util.IdGenerator;
import util.PasswordEncoder;

import java.util.EnumSet;
import java.util.Set;

/**
 * 用户工厂
 * 
 * 设计模式：工厂模式（Factory Pattern）
 * 
 * 设计说明：
 * 封装User对象的创建逻辑，统一处理ID生成、密码加密等
 * 
 * 答辩要点：
 * Q: 为什么需要工厂模式？
 * A: 用户创建涉及多个步骤：
 *    1. 生成唯一ID
 *    2. 加密密码
 *    3. 初始化角色集合（EnumSet）
 *    4. 设置默认值
 *    
 *    工厂模式将这些逻辑封装，使得：
 *    - 创建逻辑集中管理
 *    - 调用方无需关心创建细节
 *    - 保证对象创建的一致性
 */
public class UserFactory {
    
    /**
     * 创建普通用户（买家和/或卖家）
     */
    public static User createUser(String username, String password, Set<UserRole> roles) {
        String userId = IdGenerator.generateUserId();
        String encodedPassword = PasswordEncoder.encode(password);
        
        // 确保使用EnumSet
        Set<UserRole> enumSetRoles = roles.isEmpty() 
            ? EnumSet.noneOf(UserRole.class) 
            : EnumSet.copyOf(roles);
        
        return new User(userId, username, encodedPassword, enumSetRoles);
    }
    
    /**
     * 创建买家
     */
    public static User createBuyer(String username, String password) {
        return createUser(username, password, EnumSet.of(UserRole.BUYER));
    }
    
    /**
     * 创建卖家
     */
    public static User createSeller(String username, String password) {
        return createUser(username, password, EnumSet.of(UserRole.SELLER));
    }
    
    /**
     * 创建买家+卖家
     */
    public static User createBuyerAndSeller(String username, String password) {
        return createUser(username, password, EnumSet.of(UserRole.BUYER, UserRole.SELLER));
    }
    
    /**
     * 创建管理员
     */
    public static User createAdmin(String username, String password) {
        return createUser(username, password, EnumSet.of(UserRole.ADMIN));
    }
}


