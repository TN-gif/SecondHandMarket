package enums;

/**
 * 商品分类枚举
 */
public enum ProductCategory {
    ELECTRONICS("电子产品"),
    BOOKS("图书"),
    CLOTHING("服装"),
    SPORTS("运动"),
    DAILY("日用品"),
    OTHER("其他");
    
    private final String displayName;
    
    ProductCategory(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}


