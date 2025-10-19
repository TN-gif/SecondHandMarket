package strategy;

import java.util.List;

/**
 * 排序策略接口
 * 
 * 设计模式：策略模式（Strategy Pattern）
 * 
 * 设计说明：
 * 定义排序算法的统一接口，不同的排序规则实现不同的策略
 * 
 * 答辩要点：
 * Q: 为什么使用策略模式？
 * A: 商品排序有多种方式（价格、时间、信誉等），如果用if-else
 *    会导致代码臃肿。策略模式将每种排序逻辑封装成独立的策略，
 *    符合开闭原则，新增排序方式只需添加新策略，不修改现有代码。
 */
public interface SortStrategy<T> {
    
    /**
     * 排序方法
     * 
     * @param list 待排序的列表（原地排序）
     */
    void sort(List<T> list);
}


