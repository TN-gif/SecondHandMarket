package service;

import entity.Order;
import entity.Product;
import entity.User;
import enums.OrderStatus;
import enums.UserRole;
import exception.BusinessException;
import exception.PermissionDeniedException;
import exception.ResourceNotFoundException;
import repository.DataCenter;
import util.IdGenerator;

import java.util.List;

/**
 * 订单服务
 * 
 * 核心功能：
 * 1. 创建订单（下单）
 * 2. 确认订单（卖家）
 * 3. 确认收货（买家）
 * 4. 取消订单
 * 5. 订单查询
 * 
 * 答辩要点：
 * - RESERVED状态流转：下单时商品变为RESERVED，取消时恢复AVAILABLE
 * - 权限校验：买家下单、卖家确认、买家收货
 * - 观察者通知：订单状态变化时通知买卖双方
 */
public class OrderService {
    
    private final DataCenter dataCenter;
    private final NotificationService notificationService;
    
    public OrderService(NotificationService notificationService) {
        this.dataCenter = DataCenter.getInstance();
        this.notificationService = notificationService;
    }
    
    // ========== 创建订单 ==========
    
    /**
     * 创建订单（买家下单）
     */
    public Order createOrder(User buyer, String productId) {
        // 1. 权限校验：必须是买家
        if (!buyer.hasRole(UserRole.BUYER)) {
            throw new PermissionDeniedException("只有买家可以下单");
        }
        
        // 2. 查找商品
        Product product = dataCenter.findProductById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在"));
        
        // 3. 商品状态检查
        if (!product.isAvailable()) {
            throw new BusinessException("商品当前不可购买");
        }
        
        // 4. 不能购买自己的商品
        if (product.getSellerId().equals(buyer.getUserId())) {
            throw new BusinessException("不能购买自己的商品");
        }
        
        // 5. 创建订单
        String orderId = IdGenerator.generateOrderId();
        Order order = new Order(orderId, productId, buyer.getUserId(), 
                               product.getSellerId(), product.getPrice());
        
        // 6. 商品状态变为RESERVED（关键：RESERVED中间状态）
        product.reserve();
        
        // 7. 保存订单
        dataCenter.addOrder(order);
        
        // 8. 发送通知（观察者模式）
        notificationService.notify(buyer.getUserId(), 
            String.format("订单创建成功！订单号：%s", orderId));
        notificationService.notify(product.getSellerId(), 
            String.format("您有新订单！订单号：%s，商品：%s", orderId, product.getTitle()));
        
        return order;
    }
    
    // ========== 订单确认 ==========
    
    /**
     * 卖家确认订单
     */
    public void confirmOrder(User seller, String orderId) {
        // 1. 权限校验：必须是卖家
        if (!seller.hasRole(UserRole.SELLER)) {
            throw new PermissionDeniedException("只有卖家可以确认订单");
        }
        
        // 2. 查找订单
        Order order = getOrderById(orderId);
        
        // 3. 权限校验：只能确认自己的订单
        if (!order.getSellerId().equals(seller.getUserId())) {
            throw new PermissionDeniedException("只能确认自己的订单");
        }
        
        // 4. 确认订单
        order.confirm();
        
        // 5. 发送通知（买卖双方）
        Product product = dataCenter.findProductById(order.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在"));
        
        notificationService.notify(order.getBuyerId(), 
            String.format("卖家已确认订单：%s，商品《%s》等待发货", orderId, product.getTitle()));
        notificationService.notify(order.getSellerId(), 
            String.format("您已确认订单：%s，请尽快发货", orderId));
    }
    
    /**
     * 买家确认收货
     */
    public void confirmReceipt(User buyer, String orderId) {
        // 1. 权限校验：必须是买家
        if (!buyer.hasRole(UserRole.BUYER)) {
            throw new PermissionDeniedException("只有买家可以确认收货");
        }
        
        // 2. 查找订单
        Order order = getOrderById(orderId);
        
        // 3. 权限校验：只能确认自己的订单
        if (!order.getBuyerId().equals(buyer.getUserId())) {
            throw new PermissionDeniedException("只能确认自己的订单");
        }
        
        // 4. 确认收货
        order.complete();
        
        // 5. 商品状态变为SOLD
        Product product = dataCenter.findProductById(order.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在"));
        product.markAsSold();
        
        // 6. 增加买卖双方信誉分（订单完成奖励）
        User seller = dataCenter.findUserById(order.getSellerId())
                .orElseThrow(() -> new ResourceNotFoundException("卖家不存在"));
        buyer.increaseReputation(1);
        seller.increaseReputation(1);
        
        // 7. 发送通知（买卖双方）
        notificationService.notify(order.getSellerId(), 
            String.format("买家已确认收货，订单完成：%s，商品《%s》交易成功！信誉+1", orderId, product.getTitle()));
        notificationService.notify(order.getBuyerId(), 
            String.format("交易完成！订单：%s，信誉+1，欢迎评价", orderId));
    }
    
    // ========== 取消订单 ==========
    
    /**
     * 取消订单
     */
    public void cancelOrder(User user, String orderId, String reason) {
        // 1. 查找订单
        Order order = getOrderById(orderId);
        
        // 2. 权限校验：只有买家或卖家可以取消
        if (!order.getBuyerId().equals(user.getUserId()) 
            && !order.getSellerId().equals(user.getUserId())) {
            throw new PermissionDeniedException("只能取消自己的订单");
        }
        
        // 3. 取消订单
        order.cancel(reason);
        
        // 4. 商品状态恢复为AVAILABLE（关键：从RESERVED恢复）
        Product product = dataCenter.findProductById(order.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在"));
        product.cancelReservation();
        
        // 5. 信誉分变化（取消方-5分，被动方+2分）
        User buyer = dataCenter.findUserById(order.getBuyerId())
                .orElseThrow(() -> new ResourceNotFoundException("买家不存在"));
        User seller = dataCenter.findUserById(order.getSellerId())
                .orElseThrow(() -> new ResourceNotFoundException("卖家不存在"));
        
        boolean isBuyerCancel = order.getBuyerId().equals(user.getUserId());
        if (isBuyerCancel) {
            buyer.decreaseReputation(5);
            seller.increaseReputation(2);
        } else {
            seller.decreaseReputation(5);
            buyer.increaseReputation(2);
        }
        
        // 6. 发送通知
        String cancelBy = isBuyerCancel ? "买家" : "卖家";
        notificationService.notify(order.getBuyerId(), 
            String.format("订单已取消：%s（%s取消，原因：%s）信誉变化：%s", 
                orderId, cancelBy, reason, isBuyerCancel ? "-5" : "+2"));
        notificationService.notify(order.getSellerId(), 
            String.format("订单已取消：%s（%s取消，原因：%s）信誉变化：%s", 
                orderId, cancelBy, reason, isBuyerCancel ? "+2" : "-5"));
    }
    
    // ========== 订单查询 ==========
    
    /**
     * 根据ID获取订单
     */
    public Order getOrderById(String orderId) {
        return dataCenter.findOrderById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));
    }
    
    /**
     * 获取买家的订单列表
     */
    public List<Order> getOrdersByBuyer(String buyerId) {
        return dataCenter.findOrdersByBuyer(buyerId);
    }
    
    /**
     * 获取卖家的订单列表
     */
    public List<Order> getOrdersBySeller(String sellerId) {
        return dataCenter.findOrdersBySeller(sellerId);
    }
    
    /**
     * 检查订单是否可以评价
     */
    public boolean canReview(String orderId) {
        Order order = getOrderById(orderId);
        return order.canBeReviewed();
    }
}


