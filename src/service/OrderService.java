package service;

import entity.Order;
import entity.Product;
import entity.User;
import enums.OrderStatus;
import enums.UserRole;
import enums.UserStatus;
import exception.BusinessException;
import exception.PermissionDeniedException;
import exception.ResourceNotFoundException;
import repository.DataCenter;
import util.IdGenerator;
import util.SimpleLogger;
import util.ValidationUtils;

import java.util.List;

/**
 * 订单服务
 * 
 * 负责订单全生命周期管理，包括下单、确认、收货和取消。
 * 商品状态与订单状态同步：下单时商品变为RESERVED，取消时恢复AVAILABLE，完成时变为SOLD。
 * 使用观察者模式，订单状态变化时自动通知买卖双方。
 * 订单取消会影响双方信誉分：取消方-5分，被动方+2分。
 */
public class OrderService {
    
    private static final SimpleLogger logger = SimpleLogger.getLogger(OrderService.class);
    private static final int REPUTATION_PENALTY_CANCEL = 5;
    private static final int REPUTATION_REWARD_PASSIVE = 2;
    private static final int REPUTATION_REWARD_COMPLETE = 1;
    
    private final DataCenter dataCenter;
    private final NotificationService notificationService;
    
    public OrderService(NotificationService notificationService) {
        this.dataCenter = DataCenter.getInstance();
        this.notificationService = notificationService;
    }
    
    /**
     * 创建订单（买家下单）
     * 
     * 创建订单后，商品状态自动变为RESERVED，防止重复下单。
     * 同时向买卖双方发送通知。
     * 
     * @param buyer 买家，必须拥有买家角色且账号正常
     * @param productId 商品ID
     * @return 创建的订单对象
     * @throws PermissionDeniedException 如果用户状态异常或没有买家角色
     * @throws ResourceNotFoundException 如果商品不存在
     * @throws BusinessException 如果商品不可购买或试图购买自己的商品
     */
    public Order createOrder(User buyer, String productId) {
        logger.info("创建订单请求: buyerId={}, productId={}", buyer.getUserId(), productId);
        
        try {
            validateBuyerStatus(buyer);
            Product product = validateProductForPurchase(buyer, productId);
            
            String orderId = IdGenerator.generateOrderId();
            Order order = new Order(orderId, productId, buyer.getUserId(), 
                                   product.getSellerId(), product.getPrice());
            
            product.reserve();
            dataCenter.addOrder(order);
            
            logger.info("创建订单成功: orderId={}", orderId);
            notifyOrderCreated(order, product);
            
            return order;
            
        } catch (Exception e) {
            logger.error("创建订单失败: buyerId={}, productId={}, error={}", 
                        buyer.getUserId(), productId, e.getMessage());
            throw e;
        }
    }
    
    /**
     * 卖家确认订单
     * 
     * 卖家确认后订单状态变为CONFIRMED，表示准备发货。
     * 
     * @param seller 卖家
     * @param orderId 订单ID
     * @throws PermissionDeniedException 如果不是卖家或不是订单所有者
     */
    public void confirmOrder(User seller, String orderId) {
        if (!seller.hasRole(UserRole.SELLER)) {
            throw new PermissionDeniedException("只有卖家才能确认订单");
        }
        
        Order order = getOrderById(orderId);
        
        if (!order.getSellerId().equals(seller.getUserId())) {
            throw new PermissionDeniedException("只能确认自己的订单");
        }
        
        order.confirm();
        
        Product product = dataCenter.findProductById(order.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在"));
        
        notificationService.notify(order.getBuyerId(), 
            String.format("卖家已确认订单：%s，商品《%s》等待发货", orderId, product.getTitle()));
        notificationService.notify(order.getSellerId(), 
            String.format("您已确认订单：%s，请尽快发货", orderId));
    }
    
    /**
     * 买家确认收货
     * 
     * 确认收货后：
     * 1. 订单状态变为COMPLETED
     * 2. 商品状态变为SOLD
     * 3. 买卖双方各增加1点信誉分
     * 
     * @param buyer 买家
     * @param orderId 订单ID
     * @throws PermissionDeniedException 如果不是买家或不是订单所有者
     */
    public void confirmReceipt(User buyer, String orderId) {
        if (!buyer.hasRole(UserRole.BUYER)) {
            throw new PermissionDeniedException("只有买家才能确认收货");
        }
        
        Order order = getOrderById(orderId);
        
        if (!order.getBuyerId().equals(buyer.getUserId())) {
            throw new PermissionDeniedException("只能确认自己的订单");
        }
        
        order.complete();
        
        Product product = dataCenter.findProductById(order.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在"));
        product.markAsSold();
        
        User seller = dataCenter.findUserById(order.getSellerId())
                .orElseThrow(() -> new ResourceNotFoundException("卖家不存在"));
        
        updateReputationOnComplete(buyer, seller);
        notifyOrderCompleted(order, product);
    }
    
    /**
     * 取消订单
     * 
     * 取消后商品状态恢复为AVAILABLE，可重新购买。
     * 信誉分影响：取消方-5分，被动方+2分。
     * 
     * @param user 取消用户（买家或卖家）
     * @param orderId 订单ID
     * @param reason 取消理由，5-200字符
     * @throws PermissionDeniedException 如果不是订单参与方
     * @throws BusinessException 如果取消理由格式无效
     */
    public void cancelOrder(User user, String orderId, String reason) {
        logger.info("取消订单请求: orderId={}, userId={}", orderId, user.getUserId());
        
        try {
            ValidationUtils.validateCancelReason(reason);
            
            Order order = getOrderById(orderId);
            validateCancelPermission(user, order);
            
            order.cancel(reason);
            
            Product product = restoreProductStatus(order);
            User buyer = dataCenter.findUserById(order.getBuyerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Buyer does not exist"));
            User seller = dataCenter.findUserById(order.getSellerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Seller does not exist"));
            
            boolean isBuyerCancel = order.getBuyerId().equals(user.getUserId());
            updateReputationOnCancel(buyer, seller, isBuyerCancel);
            notifyOrderCancelled(order, reason, isBuyerCancel);
            
            logger.info("订单已取消: orderId={}, cancelledBy={}", orderId, isBuyerCancel ? "买家" : "卖家");
                    
        } catch (Exception e) {
            logger.error("取消订单失败: orderId={}, userId={}, error={}", 
                        orderId, user.getUserId(), e.getMessage());
            throw e;
        }
    }
    
    /**
     * 根据ID获取订单
     * 
     * @param orderId 订单ID
     * @return 订单对象
     * @throws ResourceNotFoundException 如果订单不存在
     */
    public Order getOrderById(String orderId) {
        return dataCenter.findOrderById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));
    }
    
    /**
     * 获取买家的所有订单
     * 
     * @param buyerId 买家ID
     * @return 订单列表
     */
    public List<Order> getOrdersByBuyer(String buyerId) {
        return dataCenter.findOrdersByBuyer(buyerId);
    }
    
    /**
     * 获取卖家的所有订单
     * 
     * @param sellerId 卖家ID
     * @return 订单列表
     */
    public List<Order> getOrdersBySeller(String sellerId) {
        return dataCenter.findOrdersBySeller(sellerId);
    }
    
    /**
     * 检查订单是否可以评价
     * 
     * @param orderId 订单ID
     * @return 如果订单已完成且未评价返回true
     */
    public boolean canReview(String orderId) {
        Order order = getOrderById(orderId);
        return order.canBeReviewed();
    }
    
    /**
     * 验证买家状态
     * 
     * 被封禁或删除的账号无法下单。
     */
    private void validateBuyerStatus(User buyer) {
        if (buyer.getStatus() == UserStatus.BANNED) {
            throw new PermissionDeniedException("您的账号已被封禁，无法下单");
        }
        if (buyer.getStatus() == UserStatus.DELETED) {
            throw new PermissionDeniedException("账号已被删除");
        }
        if (!buyer.hasRole(UserRole.BUYER)) {
            throw new PermissionDeniedException("只有买家才能下单");
        }
    }
    
    /**
     * 验证商品是否可购买
     * 
     * 检查商品存在性、状态和所有权。
     */
    private Product validateProductForPurchase(User buyer, String productId) {
        Product product = dataCenter.findProductById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在"));
        
        if (!product.isAvailable()) {
            throw new BusinessException("商品当前不可购买");
        }
        
        if (product.getSellerId().equals(buyer.getUserId())) {
            throw new BusinessException("不能购买自己的商品");
        }
        
        return product;
    }
    
    /**
     * 验证取消权限
     * 
     * 只有订单的买家或卖家可以取消订单。
     */
    private void validateCancelPermission(User user, Order order) {
        if (!order.getBuyerId().equals(user.getUserId()) 
            && !order.getSellerId().equals(user.getUserId())) {
            throw new PermissionDeniedException("只能取消自己的订单");
        }
    }
    
    /**
     * 恢复商品状态为可售
     * 
     * 订单取消时，将商品从RESERVED恢复为AVAILABLE。
     */
    private Product restoreProductStatus(Order order) {
        Product product = dataCenter.findProductById(order.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product does not exist"));
        product.cancelReservation();
        return product;
    }
    
    /**
     * 更新订单完成时的信誉分
     * 
     * 买卖双方各增加1点信誉分。
     */
    private void updateReputationOnComplete(User buyer, User seller) {
        buyer.increaseReputation(REPUTATION_REWARD_COMPLETE);
        seller.increaseReputation(REPUTATION_REWARD_COMPLETE);
    }
    
    /**
     * 更新订单取消时的信誉分
     * 
     * 取消方减5分，被动方加2分。
     */
    private void updateReputationOnCancel(User buyer, User seller, boolean isBuyerCancel) {
        if (isBuyerCancel) {
            buyer.decreaseReputation(REPUTATION_PENALTY_CANCEL);
            seller.increaseReputation(REPUTATION_REWARD_PASSIVE);
        } else {
            seller.decreaseReputation(REPUTATION_PENALTY_CANCEL);
            buyer.increaseReputation(REPUTATION_REWARD_PASSIVE);
        }
    }
    
    /**
     * 发送订单创建通知
     */
    private void notifyOrderCreated(Order order, Product product) {
        notificationService.notify(order.getBuyerId(), 
            String.format("订单创建成功！订单号：%s", order.getOrderId()));
        notificationService.notify(order.getSellerId(), 
            String.format("您有新订单！订单号：%s，商品：%s", order.getOrderId(), product.getTitle()));
    }
    
    /**
     * 发送订单完成通知
     */
    private void notifyOrderCompleted(Order order, Product product) {
        notificationService.notify(order.getSellerId(), 
            String.format("买家已确认收货，订单完成：%s，商品《%s》交易成功！信誉+1", 
                order.getOrderId(), product.getTitle()));
        notificationService.notify(order.getBuyerId(), 
            String.format("交易完成！订单：%s，信誉+1，欢迎评价", order.getOrderId()));
    }
    
    /**
     * 发送订单取消通知
     */
    private void notifyOrderCancelled(Order order, String reason, boolean isBuyerCancel) {
        String cancelBy = isBuyerCancel ? "买家" : "卖家";
        String buyerRepChange = isBuyerCancel ? "-5" : "+2";
        String sellerRepChange = isBuyerCancel ? "+2" : "-5";
        
        notificationService.notify(order.getBuyerId(), 
            String.format("订单已取消：%s（%s取消，理由：%s）信誉：%s", 
                order.getOrderId(), cancelBy, reason, buyerRepChange));
        notificationService.notify(order.getSellerId(), 
            String.format("订单已取消：%s（%s取消，理由：%s）信誉：%s", 
                order.getOrderId(), cancelBy, reason, sellerRepChange));
    }
}


