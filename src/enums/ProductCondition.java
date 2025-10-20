package enums;

/**
 * 商品成色枚举
 */
public enum ProductCondition {
    BRAND_NEW("全新", "未开封或几乎全新"),
    LIKE_NEW("几乎全新", "很少使用，无明显磨损"),
    GOOD("良好", "轻微使用痕迹"),
    ACCEPTABLE("可接受", "有明显磨损但功能完好");
    
    private final String displayName;
    private final String description;
    
    ProductCondition(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
}


