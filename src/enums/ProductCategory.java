package enums;

/**
 * 商品分类枚举
 */
public enum ProductCategory {
    ELECTRONICS("电子产品"),
    BOOKS("图书教材"),
    CLOTHING("服装鞋帽"),
    SPORTS("运动器材"),
    DAILY("生活用品"),
    OTHER("其他");
    
    private final String displayName;
    
    ProductCategory(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}


