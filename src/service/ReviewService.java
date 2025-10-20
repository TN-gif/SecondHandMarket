package service;

import entity.Order;
import entity.Product;
import entity.Review;
import entity.User;
import enums.UserRole;
import enums.UserStatus;
import exception.BusinessException;
import exception.PermissionDeniedException;
import exception.ResourceNotFoundException;
import repository.DataCenter;
import util.IdGenerator;
import util.InputValidator;
import util.SimpleLogger;
import util.ValidationUtils;

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
    
    private static final SimpleLogger logger = SimpleLogger.getLogger(ReviewService.class);
    
    private final DataCenter dataCenter;
    private final NotificationService notificationService;
    
    public ReviewService() {
        this.dataCenter = DataCenter.getInstance();
        this.notificationService = new NotificationService();
    }
    
    // ========== 创建评价 ==========
    
    /**
     * 创建评价
     */
    public Review createReview(User buyer, String orderId, int rating, String content) {
        // 1. 用户状态检查
        if (buyer.getStatus() == UserStatus.BANNED) {
            throw new PermissionDeniedException("您的账号已被封禁，无法发表评价");
        }
        if (buyer.getStatus() == UserStatus.DELETED) {
            throw new PermissionDeniedException("账号已被删除");
        }
        
        // 2. 权限校验：必须是买家
        if (!buyer.hasRole(UserRole.BUYER)) {
            throw new PermissionDeniedException("只有买家才能发表评价");
        }
        
        // 3. 查找订单
        Order order = dataCenter.findOrderById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));
        
        // 4. 权限校验：只能评价自己的订单
        if (!order.getBuyerId().equals(buyer.getUserId())) {
            throw new PermissionDeniedException("只能评价自己的订单");
        }
        
        // 5. 订单状态检查：只有已完成的订单可以评价
        if (!order.canBeReviewed()) {
            throw new BusinessException("只能评价已完成的订单");
        }
        
        // 6. 检查是否已经评价过
        if (dataCenter.findReviewByOrderId(orderId).isPresent()) {
            throw new BusinessException("该订单已经评价过了");
        }
        
        // 7. 验证评分
        if (!InputValidator.isValidRating(rating)) {
            throw new BusinessException("评分必须在1-5星之间");
        }
        
        // 8. 创建评价
        String reviewId = IdGenerator.generateReviewId();
        Review review = new Review(reviewId, orderId, order.getProductId(),
                                  buyer.getUserId(), order.getSellerId(), rating, content);
        
        // 9. 保存评价
        dataCenter.addReview(review);
        
        // 10. 更新卖家信誉分
        updateSellerReputation(order.getSellerId(), rating);
        
        // 11. 如果是低分评价（1-2星），额外通知卖家注意
        if (rating <= 2) {
            Product product = dataCenter.findProductById(order.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("商品不存在"));
            
            // 发送特殊提醒
            notificationService.notify(order.getSellerId(), 
                String.format("【低评分提醒】您收到了%d星差评！订单：%s，商品：%s，评价：%s。请提高服务质量！", 
                    rating, orderId, product.getTitle(), content));
        }
        
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
                .orElseThrow(() -> new ResourceNotFoundException("No review found for this order"));
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


