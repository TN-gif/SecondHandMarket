package strategy;

import entity.Product;
import java.util.Comparator;

/**
 * 商品排序策略集合
 * 
 * 设计模式：策略模式
 * 
 * 设计说明：
 * 提供各种常用的商品排序策略，使用Lambda表达式实现
 */
public class ProductSortStrategies {
    
    /**
     * 按价格升序排序
     */
    public static final SortStrategy<Product> BY_PRICE_ASC = 
        list -> list.sort(Comparator.comparingDouble(Product::getPrice));
    
    /**
     * 按价格降序排序
     */
    public static final SortStrategy<Product> BY_PRICE_DESC = 
        list -> list.sort(Comparator.comparingDouble(Product::getPrice).reversed());
    
    /**
     * 按发布时间升序排序（最旧的在前）
     */
    public static final SortStrategy<Product> BY_TIME_ASC = 
        list -> list.sort(Comparator.comparing(Product::getPublishTime));
    
    /**
     * 按发布时间降序排序（最新的在前）
     */
    public static final SortStrategy<Product> BY_TIME_DESC = 
        list -> list.sort(Comparator.comparing(Product::getPublishTime).reversed());
    
    /**
     * 按商品标题排序
     */
    public static final SortStrategy<Product> BY_TITLE = 
        list -> list.sort(Comparator.comparing(Product::getTitle));
}


