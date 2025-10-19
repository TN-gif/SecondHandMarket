package dto;

import enums.ProductCategory;
import enums.ProductCondition;
import enums.ProductStatus;

/**
 * 搜索条件DTO
 * 
 * 设计模式：建造者模式（Builder Pattern）
 * 
 * 设计说明：
 * 搜索条件有很多可选参数，使用建造者模式可以：
 * 1. 灵活组合参数
 * 2. 代码可读性强
 * 3. 避免多个重载构造器
 * 
 * 答辩要点：
 * Q: 为什么使用建造者模式？
 * A: 搜索条件有8个可选参数，如果用构造器会有大量重载。
 *    建造者模式让代码更清晰：
 *    new SearchCriteria.Builder()
 *        .keyword("手机")
 *        .category(ELECTRONICS)
 *        .maxPrice(1000.0)
 *        .build()
 */
public class SearchCriteria {
    
    private final String keyword;           // 关键词
    private final ProductCategory category; // 分类
    private final ProductCondition condition; // 成色
    private final ProductStatus status;     // 状态
    private final Double minPrice;          // 最低价格
    private final Double maxPrice;          // 最高价格
    private final String sellerId;          // 卖家ID
    
    /**
     * 私有构造器，只能通过Builder创建
     */
    private SearchCriteria(Builder builder) {
        this.keyword = builder.keyword;
        this.category = builder.category;
        this.condition = builder.condition;
        this.status = builder.status;
        this.minPrice = builder.minPrice;
        this.maxPrice = builder.maxPrice;
        this.sellerId = builder.sellerId;
    }
    
    // ========== Getters ==========
    
    public String getKeyword() {
        return keyword;
    }
    
    public ProductCategory getCategory() {
        return category;
    }
    
    public ProductCondition getCondition() {
        return condition;
    }
    
    public ProductStatus getStatus() {
        return status;
    }
    
    public Double getMinPrice() {
        return minPrice;
    }
    
    public Double getMaxPrice() {
        return maxPrice;
    }
    
    public String getSellerId() {
        return sellerId;
    }
    
    // ========== 建造者 ==========
    
    /**
     * 建造者类
     */
    public static class Builder {
        private String keyword;
        private ProductCategory category;
        private ProductCondition condition;
        private ProductStatus status;
        private Double minPrice;
        private Double maxPrice;
        private String sellerId;
        
        public Builder keyword(String keyword) {
            this.keyword = keyword;
            return this;
        }
        
        public Builder category(ProductCategory category) {
            this.category = category;
            return this;
        }
        
        public Builder condition(ProductCondition condition) {
            this.condition = condition;
            return this;
        }
        
        public Builder status(ProductStatus status) {
            this.status = status;
            return this;
        }
        
        public Builder minPrice(Double minPrice) {
            this.minPrice = minPrice;
            return this;
        }
        
        public Builder maxPrice(Double maxPrice) {
            this.maxPrice = maxPrice;
            return this;
        }
        
        public Builder sellerId(String sellerId) {
            this.sellerId = sellerId;
            return this;
        }
        
        /**
         * 构建SearchCriteria对象
         */
        public SearchCriteria build() {
            // 可以在这里添加验证逻辑
            if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
                throw new IllegalArgumentException("最低价格不能大于最高价格");
            }
            return new SearchCriteria(this);
        }
    }
}


