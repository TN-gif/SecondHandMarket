package enums;

/**
 * 用户状态枚举
 */
public enum UserStatus {
    /**
     * 正常状态
     */
    ACTIVE("正常"),
    
    /**
     * 被封禁
     */
    BANNED("已封禁"),
    
    /**
     * 已注销
     */
    DELETED("已注销");
    
    private final String displayName;
    
    UserStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}


