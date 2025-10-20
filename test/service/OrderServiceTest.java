package service;

import entity.Order;
import entity.Product;
import entity.User;
import enums.OrderStatus;
import enums.ProductCategory;
import enums.ProductCondition;
import enums.ProductStatus;
import enums.UserRole;
import exception.BusinessException;
import exception.PermissionDeniedException;
import repository.DataCenter;

import java.util.EnumSet;
import java.util.Set;

/**
 * OrderService Unit Tests
 * 
 * Test Coverage:
 * 1. Create Order (Success / Failure scenarios)
 * 2. Confirm Order (Seller)
 * 3. Confirm Receipt (Buyer)
 * 4. Cancel Order
 * 5. Product Status Flow (AVAILABLE -> RESERVED -> SOLD/AVAILABLE)
 */
public class OrderServiceTest {
    
    private OrderService orderService;
    private ProductService productService;
    private UserService userService;
    private NotificationService notificationService;
    
    public void setUp() {
        DataCenter.getInstance().clearAll();
        
        notificationService = new NotificationService();
        userService = new UserService(notificationService);
        productService = new ProductService();
        orderService = new OrderService(notificationService);
        
        System.out.println("[TEST] Setup completed");
    }
    
    public void tearDown() {
        DataCenter.getInstance().clearAll();
        System.out.println("[TEST] Teardown completed");
    }
    
    private User createTestBuyer(String username) {
        Set<UserRole> roles = EnumSet.of(UserRole.BUYER);
        return userService.register(username, "password123", roles);
    }
    
    private User createTestSeller(String username) {
        Set<UserRole> roles = EnumSet.of(UserRole.SELLER);
        return userService.register(username, "password123", roles);
    }
    
    private Product createTestProduct(User seller) {
        return productService.publishProduct(
            seller, "iPhone 13", "Like new", 
            3999.99, ProductCategory.ELECTRONICS, ProductCondition.LIKE_NEW
        );
    }
    
    // ========== Create Order Tests ==========
    
    public void testCreateOrder_ValidInput_Success() {
        setUp();
        try {
            User buyer = createTestBuyer("buyer1");
            User seller = createTestSeller("seller1");
            Product product = createTestProduct(seller);
            
            assert product.getStatus() == ProductStatus.AVAILABLE : "Product should be AVAILABLE";
            
            Order order = orderService.createOrder(buyer, product.getProductId());
            
            assert order != null : "Order should not be null";
            assert buyer.getUserId().equals(order.getBuyerId()) : "Buyer ID should match";
            assert seller.getUserId().equals(order.getSellerId()) : "Seller ID should match";
            assert order.getStatus() == OrderStatus.PENDING : "Order status should be PENDING";
            assert product.getStatus() == ProductStatus.RESERVED : "Product should be RESERVED";
            
            System.out.println("[PASS] testCreateOrder_ValidInput_Success");
        } catch (Exception e) {
            System.out.println("[FAIL] testCreateOrder_ValidInput: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testCreateOrder_NotBuyer_ThrowsException() {
        setUp();
        try {
            User seller1 = createTestSeller("seller1");
            User seller2 = createTestSeller("seller2");
            Product product = createTestProduct(seller1);
            
            try {
                orderService.createOrder(seller2, product.getProductId());
                System.out.println("[FAIL] testCreateOrder_NotBuyer: Should throw PermissionDeniedException");
            } catch (PermissionDeniedException e) {
                System.out.println("[PASS] testCreateOrder_NotBuyer_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testCreateOrder_NotBuyer: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testCreateOrder_OwnProduct_ThrowsException() {
        setUp();
        try {
            Set<UserRole> roles = EnumSet.of(UserRole.BUYER, UserRole.SELLER);
            User user = userService.register("user1", "password123", roles);
            Product product = createTestProduct(user);
            
            try {
                orderService.createOrder(user, product.getProductId());
                System.out.println("[FAIL] testCreateOrder_OwnProduct: Should throw BusinessException");
            } catch (BusinessException e) {
                System.out.println("[PASS] testCreateOrder_OwnProduct_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testCreateOrder_OwnProduct: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testCreateOrder_ProductNotAvailable_ThrowsException() {
        setUp();
        try {
            User buyer = createTestBuyer("buyer1");
            User seller = createTestSeller("seller1");
            Product product = createTestProduct(seller);
            
            // Remove product
            productService.removeProduct(seller, product.getProductId());
            
            try {
                orderService.createOrder(buyer, product.getProductId());
                System.out.println("[FAIL] testCreateOrder_ProductNotAvailable: Should throw BusinessException");
            } catch (BusinessException e) {
                System.out.println("[PASS] testCreateOrder_ProductNotAvailable_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testCreateOrder_ProductNotAvailable: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    // ========== Cancel Order Tests ==========
    
    public void testCancelOrder_ByBuyer_ReputationChange() {
        setUp();
        try {
            User buyer = createTestBuyer("buyer1");
            User seller = createTestSeller("seller1");
            Product product = createTestProduct(seller);
            
            Order order = orderService.createOrder(buyer, product.getProductId());
            assert product.getStatus() == ProductStatus.RESERVED : "Product should be RESERVED";
            
            int buyerRepBefore = buyer.getReputation();
            int sellerRepBefore = seller.getReputation();
            
            // Buyer cancels order
            orderService.cancelOrder(buyer, order.getOrderId(), "Changed my mind");
            
            assert order.getStatus() == OrderStatus.CANCELLED : "Order should be CANCELLED";
            assert product.getStatus() == ProductStatus.AVAILABLE : "Product should be AVAILABLE again";
            assert buyer.getReputation() == buyerRepBefore - 5 : "Buyer reputation should decrease by 5";
            assert seller.getReputation() == sellerRepBefore + 2 : "Seller reputation should increase by 2";
            
            System.out.println("[PASS] testCancelOrder_ByBuyer_ReputationChange");
        } catch (Exception e) {
            System.out.println("[FAIL] testCancelOrder_ByBuyer: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testCancelOrder_BySeller_ReputationChange() {
        setUp();
        try {
            User buyer = createTestBuyer("buyer1");
            User seller = createTestSeller("seller1");
            Product product = createTestProduct(seller);
            
            Order order = orderService.createOrder(buyer, product.getProductId());
            
            int buyerRepBefore = buyer.getReputation();
            int sellerRepBefore = seller.getReputation();
            
            // Seller cancels order
            orderService.cancelOrder(seller, order.getOrderId(), "Product damaged");
            
            assert order.getStatus() == OrderStatus.CANCELLED : "Order should be CANCELLED";
            assert product.getStatus() == ProductStatus.AVAILABLE : "Product should be AVAILABLE again";
            assert seller.getReputation() == sellerRepBefore - 5 : "Seller reputation should decrease by 5";
            assert buyer.getReputation() == buyerRepBefore + 2 : "Buyer reputation should increase by 2";
            
            System.out.println("[PASS] testCancelOrder_BySeller_ReputationChange");
        } catch (Exception e) {
            System.out.println("[FAIL] testCancelOrder_BySeller: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    // ========== Confirm Receipt Tests ==========
    
    public void testConfirmReceipt_Success_ProductSold() {
        setUp();
        try {
            User buyer = createTestBuyer("buyer1");
            User seller = createTestSeller("seller1");
            Product product = createTestProduct(seller);
            
            Order order = orderService.createOrder(buyer, product.getProductId());
            
            // Seller confirms
            orderService.confirmOrder(seller, order.getOrderId());
            assert order.getStatus() == OrderStatus.CONFIRMED : "Order should be CONFIRMED";
            
            int buyerRepBefore = buyer.getReputation();
            int sellerRepBefore = seller.getReputation();
            
            // Buyer confirms receipt
            orderService.confirmReceipt(buyer, order.getOrderId());
            
            assert order.getStatus() == OrderStatus.COMPLETED : "Order should be COMPLETED";
            assert product.getStatus() == ProductStatus.SOLD : "Product should be SOLD";
            assert buyer.getReputation() == buyerRepBefore + 1 : "Buyer reputation should increase by 1";
            assert seller.getReputation() == sellerRepBefore + 1 : "Seller reputation should increase by 1";
            
            System.out.println("[PASS] testConfirmReceipt_Success_ProductSold");
        } catch (Exception e) {
            System.out.println("[FAIL] testConfirmReceipt_Success: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    // ========== User Status Tests ==========
    
    public void testCreateOrder_BannedBuyer_ThrowsException() {
        setUp();
        try {
            User buyer = createTestBuyer("buyer1");
            User seller = createTestSeller("seller1");
            Product product = createTestProduct(seller);
            
            buyer.setStatus(enums.UserStatus.BANNED);
            
            try {
                orderService.createOrder(buyer, product.getProductId());
                System.out.println("[FAIL] testCreateOrder_BannedBuyer: Should throw PermissionDeniedException");
            } catch (PermissionDeniedException e) {
                System.out.println("[PASS] testCreateOrder_BannedBuyer_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testCreateOrder_BannedBuyer: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testCreateOrder_SameProductTwice_ThrowsException() {
        setUp();
        try {
            User buyer = createTestBuyer("buyer1");
            User seller = createTestSeller("seller1");
            Product product = createTestProduct(seller);
            
            // First order succeeds
            orderService.createOrder(buyer, product.getProductId());
            
            try {
                // Second order on same product should fail (already RESERVED)
                orderService.createOrder(buyer, product.getProductId());
                System.out.println("[FAIL] testCreateOrder_SameProductTwice: Should throw BusinessException");
            } catch (BusinessException e) {
                System.out.println("[PASS] testCreateOrder_SameProductTwice_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testCreateOrder_SameProductTwice: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testCreateOrder_ProductNotExists_ThrowsException() {
        setUp();
        try {
            User buyer = createTestBuyer("buyer1");
            
            try {
                orderService.createOrder(buyer, "P999");
                System.out.println("[FAIL] testCreateOrder_ProductNotExists: Should throw exception");
            } catch (Exception e) {
                System.out.println("[PASS] testCreateOrder_ProductNotExists_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testCreateOrder_ProductNotExists: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    // ========== Confirm Order Tests ==========
    
    public void testConfirmOrder_NotSeller_ThrowsException() {
        setUp();
        try {
            User buyer1 = createTestBuyer("buyer1");
            User buyer2 = createTestBuyer("buyer2");
            User seller = createTestSeller("seller1");
            Product product = createTestProduct(seller);
            
            Order order = orderService.createOrder(buyer1, product.getProductId());
            
            try {
                orderService.confirmOrder(buyer2, order.getOrderId());
                System.out.println("[FAIL] testConfirmOrder_NotSeller: Should throw PermissionDeniedException");
            } catch (PermissionDeniedException e) {
                System.out.println("[PASS] testConfirmOrder_NotSeller_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testConfirmOrder_NotSeller: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testConfirmOrder_NotOwner_ThrowsException() {
        setUp();
        try {
            User buyer = createTestBuyer("buyer1");
            User seller1 = createTestSeller("seller1");
            User seller2 = createTestSeller("seller2");
            Product product = createTestProduct(seller1);
            
            Order order = orderService.createOrder(buyer, product.getProductId());
            
            try {
                orderService.confirmOrder(seller2, order.getOrderId());
                System.out.println("[FAIL] testConfirmOrder_NotOwner: Should throw PermissionDeniedException");
            } catch (PermissionDeniedException e) {
                System.out.println("[PASS] testConfirmOrder_NotOwner_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testConfirmOrder_NotOwner: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    // ========== Confirm Receipt Tests ==========
    
    public void testConfirmReceipt_NotBuyer_ThrowsException() {
        setUp();
        try {
            User buyer = createTestBuyer("buyer1");
            User seller1 = createTestSeller("seller1");
            User seller2 = createTestSeller("seller2");
            Product product = createTestProduct(seller1);
            
            Order order = orderService.createOrder(buyer, product.getProductId());
            orderService.confirmOrder(seller1, order.getOrderId());
            
            try {
                orderService.confirmReceipt(seller2, order.getOrderId());
                System.out.println("[FAIL] testConfirmReceipt_NotBuyer: Should throw PermissionDeniedException");
            } catch (PermissionDeniedException e) {
                System.out.println("[PASS] testConfirmReceipt_NotBuyer_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testConfirmReceipt_NotBuyer: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testConfirmReceipt_NotOwner_ThrowsException() {
        setUp();
        try {
            User buyer1 = createTestBuyer("buyer1");
            User buyer2 = createTestBuyer("buyer2");
            User seller = createTestSeller("seller1");
            Product product = createTestProduct(seller);
            
            Order order = orderService.createOrder(buyer1, product.getProductId());
            orderService.confirmOrder(seller, order.getOrderId());
            
            try {
                orderService.confirmReceipt(buyer2, order.getOrderId());
                System.out.println("[FAIL] testConfirmReceipt_NotOwner: Should throw PermissionDeniedException");
            } catch (PermissionDeniedException e) {
                System.out.println("[PASS] testConfirmReceipt_NotOwner_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testConfirmReceipt_NotOwner: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    // ========== Cancel Order Validation Tests ==========
    
    public void testCancelOrder_ReasonTooShort_ThrowsException() {
        setUp();
        try {
            User buyer = createTestBuyer("buyer1");
            User seller = createTestSeller("seller1");
            Product product = createTestProduct(seller);
            
            Order order = orderService.createOrder(buyer, product.getProductId());
            
            try {
                orderService.cancelOrder(buyer, order.getOrderId(), "bad");
                System.out.println("[FAIL] testCancelOrder_ReasonTooShort: Should throw BusinessException");
            } catch (BusinessException e) {
                System.out.println("[PASS] testCancelOrder_ReasonTooShort_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testCancelOrder_ReasonTooShort: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testCancelOrder_ReasonTooLong_ThrowsException() {
        setUp();
        try {
            User buyer = createTestBuyer("buyer1");
            User seller = createTestSeller("seller1");
            Product product = createTestProduct(seller);
            
            Order order = orderService.createOrder(buyer, product.getProductId());
            String longReason = "A".repeat(201);
            
            try {
                orderService.cancelOrder(buyer, order.getOrderId(), longReason);
                System.out.println("[FAIL] testCancelOrder_ReasonTooLong: Should throw BusinessException");
            } catch (BusinessException e) {
                System.out.println("[PASS] testCancelOrder_ReasonTooLong_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testCancelOrder_ReasonTooLong: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testCancelOrder_NotOwner_ThrowsException() {
        setUp();
        try {
            User buyer1 = createTestBuyer("buyer1");
            User buyer2 = createTestBuyer("buyer2");
            User seller = createTestSeller("seller1");
            Product product = createTestProduct(seller);
            
            Order order = orderService.createOrder(buyer1, product.getProductId());
            
            try {
                orderService.cancelOrder(buyer2, order.getOrderId(), "Not my order");
                System.out.println("[FAIL] testCancelOrder_NotOwner: Should throw PermissionDeniedException");
            } catch (PermissionDeniedException e) {
                System.out.println("[PASS] testCancelOrder_NotOwner_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testCancelOrder_NotOwner: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    // ========== Query Tests ==========
    
    public void testGetOrdersByBuyer_Success() {
        setUp();
        try {
            User buyer = createTestBuyer("buyer1");
            User seller1 = createTestSeller("seller1");
            User seller2 = createTestSeller("seller2");
            
            Product product1 = createTestProduct(seller1);
            Product product2 = createTestProduct(seller2);
            
            orderService.createOrder(buyer, product1.getProductId());
            orderService.createOrder(buyer, product2.getProductId());
            
            java.util.List<Order> orders = orderService.getOrdersByBuyer(buyer.getUserId());
            
            assert orders.size() == 2 : "Should have 2 orders";
            
            System.out.println("[PASS] testGetOrdersByBuyer_Success");
        } catch (Exception e) {
            System.out.println("[FAIL] testGetOrdersByBuyer: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testGetOrdersBySeller_Success() {
        setUp();
        try {
            User buyer1 = createTestBuyer("buyer1");
            User buyer2 = createTestBuyer("buyer2");
            User seller = createTestSeller("seller1");
            
            Product product1 = createTestProduct(seller);
            Product product2 = productService.publishProduct(
                seller, "MacBook Pro", "Great", 
                8999.99, ProductCategory.ELECTRONICS, ProductCondition.BRAND_NEW
            );
            
            orderService.createOrder(buyer1, product1.getProductId());
            orderService.createOrder(buyer2, product2.getProductId());
            
            java.util.List<Order> orders = orderService.getOrdersBySeller(seller.getUserId());
            
            assert orders.size() == 2 : "Should have 2 orders";
            
            System.out.println("[PASS] testGetOrdersBySeller_Success");
        } catch (Exception e) {
            System.out.println("[FAIL] testGetOrdersBySeller: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testCanReview_CompletedOrder_ReturnsTrue() {
        setUp();
        try {
            User buyer = createTestBuyer("buyer1");
            User seller = createTestSeller("seller1");
            Product product = createTestProduct(seller);
            
            Order order = orderService.createOrder(buyer, product.getProductId());
            orderService.confirmOrder(seller, order.getOrderId());
            orderService.confirmReceipt(buyer, order.getOrderId());
            
            boolean canReview = orderService.canReview(order.getOrderId());
            
            assert canReview : "Completed order should be reviewable";
            
            System.out.println("[PASS] testCanReview_CompletedOrder_ReturnsTrue");
        } catch (Exception e) {
            System.out.println("[FAIL] testCanReview_CompletedOrder: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testCanReview_PendingOrder_ReturnsFalse() {
        setUp();
        try {
            User buyer = createTestBuyer("buyer1");
            User seller = createTestSeller("seller1");
            Product product = createTestProduct(seller);
            
            Order order = orderService.createOrder(buyer, product.getProductId());
            
            boolean canReview = orderService.canReview(order.getOrderId());
            
            assert !canReview : "Pending order should not be reviewable";
            
            System.out.println("[PASS] testCanReview_PendingOrder_ReturnsFalse");
        } catch (Exception e) {
            System.out.println("[FAIL] testCanReview_PendingOrder: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    // ========== Main Method ==========
    
    public static void main(String[] args) {
        OrderServiceTest test = new OrderServiceTest();
        
        System.out.println("========================================");
        System.out.println("Running OrderService Unit Tests");
        System.out.println("========================================\n");
        
        // Basic Create Order Tests
        test.testCreateOrder_ValidInput_Success();
        test.testCreateOrder_NotBuyer_ThrowsException();
        test.testCreateOrder_OwnProduct_ThrowsException();
        test.testCreateOrder_ProductNotAvailable_ThrowsException();
        
        // User Status Tests
        test.testCreateOrder_BannedBuyer_ThrowsException();
        test.testCreateOrder_SameProductTwice_ThrowsException();
        test.testCreateOrder_ProductNotExists_ThrowsException();
        
        // Confirm Order Tests
        test.testConfirmOrder_NotSeller_ThrowsException();
        test.testConfirmOrder_NotOwner_ThrowsException();
        
        // Confirm Receipt Tests
        test.testConfirmReceipt_Success_ProductSold();
        test.testConfirmReceipt_NotBuyer_ThrowsException();
        test.testConfirmReceipt_NotOwner_ThrowsException();
        
        // Cancel Order Tests
        test.testCancelOrder_ByBuyer_ReputationChange();
        test.testCancelOrder_BySeller_ReputationChange();
        test.testCancelOrder_ReasonTooShort_ThrowsException();
        test.testCancelOrder_ReasonTooLong_ThrowsException();
        test.testCancelOrder_NotOwner_ThrowsException();
        
        // Query Tests
        test.testGetOrdersByBuyer_Success();
        test.testGetOrdersBySeller_Success();
        test.testCanReview_CompletedOrder_ReturnsTrue();
        test.testCanReview_PendingOrder_ReturnsFalse();
        
        System.out.println("\n========================================");
        System.out.println("All OrderService Tests Completed");
        System.out.println("========================================");
    }
}

