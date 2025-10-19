package service;

import entity.Order;
import entity.Review;
import entity.User;
import enums.UserRole;
import exception.BusinessException;
import exception.PermissionDeniedException;
import exception.ResourceNotFoundException;
import repository.DataCenter;
import util.IdGenerator;
import util.InputValidator;

import java.util.List;

/**
 * 评价服务
 * 
 * 核心功能：
 * 1. 创建评价
 * 2. 查询评价
 * 3. 更新卖家信誉分
 * 
 * 答辩要点：
 * - 评价与信誉挂钩：好评+5分，差评-3分
 * - 权限校验：只有买家可以评价，且只能评价已完成的订单
 * - 一个订单只能评价一次
 */
public class ReviewService {
    
    private final DataCenter dataCenter;
    
    public ReviewService() {
        this.dataCenter = DataCenter.getInstance();
    }
    
    // ========== 创建评价 ==========
    
    /**
     * 创建评价
     */
    public Review createReview(User buyer, String orderId, int rating, String content) {
        // 1. 权限校验：必须是买家
        if (!buyer.hasRole(UserRole.BUYER)) {
            throw new PermissionDeniedException("只有买家可以评价");
        }
        
        // 2. 查找订单
        Order order = dataCenter.findOrderById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));
        
        // 3. 权限校验：只能评价自己的订单
        if (!order.getBuyerId().equals(buyer.getUserId())) {
            throw new PermissionDeniedException("只能评价自己的订单");
        }
        
        // 4. 订单状态检查：只有已完成的订单可以评价
        if (!order.canBeReviewed()) {
            throw new BusinessException("只有已完成的订单可以评价");
        }
        
        // 5. 检查是否已经评价过
        if (dataCenter.findReviewByOrderId(orderId).isPresent()) {
            throw new BusinessException("该订单已经评价过");
        }
        
        // 6. 验证评分
        if (!InputValidator.isValidRating(rating)) {
            throw new BusinessException("评分必须在1-5星之间");
        }
        
        // 7. 创建评价
        String reviewId = IdGenerator.generateReviewId();
        Review review = new Review(reviewId, orderId, order.getProductId(),
                                  buyer.getUserId(), order.getSellerId(), rating, content);
        
        // 8. 保存评价
        dataCenter.addReview(review);
        
        // 9. 更新卖家信誉分
        updateSellerReputation(order.getSellerId(), rating);
        
        return review;
    }
    
    /**
     * 更新卖家信誉分
     * 
     * 规则：
     * - 5星：+5分
     * - 4星：+2分
     * - 3星：0分
     * - 2星：-2分
     * - 1星：-3分
     */
    private void updateSellerReputation(String sellerId, int rating) {
        User seller = dataCenter.findUserById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("卖家不存在"));
        
        int change = switch (rating) {
            case 5 -> 5;
            case 4 -> 2;
            case 3 -> 0;
            case 2 -> -2;
            case 1 -> -3;
            default -> 0;
        };
        
        if (change > 0) {
            seller.increaseReputation(change);
        } else if (change < 0) {
            seller.decreaseReputation(-change);
        }
    }
    
    // ========== 查询评价 ==========
    
    /**
     * 根据ID获取评价
     */
    public Review getReviewById(String reviewId) {
        return dataCenter.findReviewById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("评价不存在"));
    }
    
    /**
     * 根据订单ID获取评价
     */
    public Review getReviewByOrderId(String orderId) {
        return dataCenter.findReviewByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("该订单还没有评价"));
    }
    
    /**
     * 获取卖家的所有评价
     */
    public List<Review> getReviewsBySeller(String sellerId) {
        return dataCenter.findReviewsByReviewee(sellerId);
    }
    
    /**
     * 计算卖家的平均评分
     */
    public double getAverageRating(String sellerId) {
        List<Review> reviews = getReviewsBySeller(sellerId);
        if (reviews.isEmpty()) {
            return 0.0;
        }
        return reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
    }
}


