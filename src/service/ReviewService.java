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
 * 负责订单评价的创建和查询。
 * 评价影响卖家信誉分：5星+5分，4星+2分，3星0分，2星-2分，1星-3分。
 * 只有买家可以评价已完成的订单，且每个订单只能评价一次。
 * 低分评价（1-2星）会向卖家发送特别提醒。
 */
public class ReviewService {
    
    private static final SimpleLogger logger = SimpleLogger.getLogger(ReviewService.class);
    
    private final DataCenter dataCenter;
    private final NotificationService notificationService;
    
    public ReviewService() {
        this.dataCenter = DataCenter.getInstance();
        this.notificationService = new NotificationService();
    }
    
    /**
     * 创建评价
     * 
     * 评价会影响卖家信誉分，低分评价（1-2星）会向卖家发送特别提醒。
     * 
     * @param buyer 买家，必须是订单的买家
     * @param orderId 订单ID，必须是已完成状态
     * @param rating 评分，1-5星
     * @param content 评价内容
     * @return 创建的评价对象
     * @throws PermissionDeniedException 如果用户状态异常、不是买家或不是订单所有者
     * @throws BusinessException 如果订单未完成、已评价过或评分无效
     */
    public Review createReview(User buyer, String orderId, int rating, String content) {
        validateReviewerStatus(buyer);
        Order order = validateOrderForReview(buyer, orderId);
        validateRating(rating);
        
        String reviewId = IdGenerator.generateReviewId();
        Review review = new Review(reviewId, orderId, order.getProductId(),
                                  buyer.getUserId(), order.getSellerId(), rating, content);
        
        dataCenter.addReview(review);
        updateSellerReputation(order.getSellerId(), rating);
        notifyLowRatingIfNeeded(order, rating, content);
        
        return review;
    }
    
    /**
     * 根据ID获取评价
     * 
     * @param reviewId 评价ID
     * @return 评价对象
     * @throws ResourceNotFoundException 如果评价不存在
     */
    public Review getReviewById(String reviewId) {
        return dataCenter.findReviewById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("评价不存在"));
    }
    
    /**
     * 根据订单ID获取评价
     * 
     * @param orderId 订单ID
     * @return 评价对象
     * @throws ResourceNotFoundException 如果评价不存在
     */
    public Review getReviewByOrderId(String orderId) {
        return dataCenter.findReviewByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("No review found for this order"));
    }
    
    /**
     * 获取卖家收到的所有评价
     * 
     * @param sellerId 卖家ID
     * @return 评价列表
     */
    public List<Review> getReviewsBySeller(String sellerId) {
        return dataCenter.findReviewsByReviewee(sellerId);
    }
    
    /**
     * 计算卖家的平均评分
     * 
     * @param sellerId 卖家ID
     * @return 平均评分，如果没有评价返回0.0
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
    
    /**
     * 验证评价者状态
     */
    private void validateReviewerStatus(User buyer) {
        if (buyer.getStatus() == UserStatus.BANNED) {
            throw new PermissionDeniedException("您的账号已被封禁，无法发表评价");
        }
        if (buyer.getStatus() == UserStatus.DELETED) {
            throw new PermissionDeniedException("账号已被删除");
        }
        if (!buyer.hasRole(UserRole.BUYER)) {
            throw new PermissionDeniedException("只有买家才能发表评价");
        }
    }
    
    /**
     * 验证订单是否可评价
     */
    private Order validateOrderForReview(User buyer, String orderId) {
        Order order = dataCenter.findOrderById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));
        
        if (!order.getBuyerId().equals(buyer.getUserId())) {
            throw new PermissionDeniedException("只能评价自己的订单");
        }
        
        if (!order.canBeReviewed()) {
            throw new BusinessException("只能评价已完成的订单");
        }
        
        if (dataCenter.findReviewByOrderId(orderId).isPresent()) {
            throw new BusinessException("该订单已经评价过了");
        }
        
        return order;
    }
    
    /**
     * 验证评分
     */
    private void validateRating(int rating) {
        if (!InputValidator.isValidRating(rating)) {
            throw new BusinessException("评分必须在1-5星之间");
        }
    }
    
    /**
     * 更新卖家信誉分
     * 
     * 根据评分调整信誉：5星+5分，4星+2分，3星0分，2星-2分，1星-3分。
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
    
    /**
     * 低分评价时发送特别通知
     * 
     * 1-2星差评会提醒卖家注意服务质量。
     */
    private void notifyLowRatingIfNeeded(Order order, int rating, String content) {
        if (rating <= 2) {
            Product product = dataCenter.findProductById(order.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("商品不存在"));
            
            notificationService.notify(order.getSellerId(), 
                String.format("【低评分提醒】您收到了%d星差评！订单：%s，商品：%s，评价：%s。请提高服务质量！", 
                    rating, order.getOrderId(), product.getTitle(), content));
        }
    }
}


