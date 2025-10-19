package enums;

/**
 * 用户角色枚举
 * 
 * 设计说明：
 * - 使用EnumSet管理用户的多个角色
 * - 一个用户可以同时是买家和卖家
 * 
 * 答辩要点：
 * 相比使用多个boolean字段（isBuyer, isSeller），EnumSet的优势：
 * 1. 类型安全：不会出现拼写错误
 * 2. 高性能：内部使用位向量，空间效率高
 * 3. 易扩展：新增角色只需添加枚举值
 * 4. 表达力强：Set<UserRole>比多个boolean更清晰
 */
public enum UserRole {
    /**
     * 买家：可以购买商品、下单、评价
     */
    BUYER("买家"),
    
    /**
     * 卖家：可以发布商品、管理商品、确认订单
     */
    SELLER("卖家"),
    
    /**
     * 管理员：拥有所有权限
     */
    ADMIN("管理员");
    
    private final String displayName;
    
    UserRole(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}


