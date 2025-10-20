package service;

import entity.Order;
import entity.Product;
import entity.Review;
import entity.User;
import enums.OrderStatus;
import enums.ProductCategory;
import enums.ProductCondition;
import enums.UserRole;
import enums.UserStatus;
import exception.BusinessException;
import exception.PermissionDeniedException;
import exception.ResourceNotFoundException;
import repository.DataCenter;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * ReviewService Unit Tests
 * 
 * Test Coverage:
 * 1. Create Review (Success / Failure scenarios)
 * 2. Reputation Update (5-star to 1-star)
 * 3. Query Reviews
 * 4. Average Rating Calculation
 */
public class ReviewServiceTest {
    
    private ReviewService reviewService;
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
        reviewService = new ReviewService();
        
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
    
    /**
     * Helper: Create a completed order ready for review
     */
    private Order createCompletedOrder(User buyer, User seller) {
        Product product = productService.publishProduct(
            seller, "iPhone 13", "Like new", 
            3999.99, ProductCategory.ELECTRONICS, ProductCondition.LIKE_NEW
        );
        
        Order order = orderService.createOrder(buyer, product.getProductId());
        orderService.confirmOrder(seller, order.getOrderId());
        orderService.confirmReceipt(buyer, order.getOrderId());
        
        return order;
    }
    
    // ========== Create Review Tests ==========
    
    public void testCreateReview_Success() {
        setUp();
        try {
            User buyer = createTestBuyer("buyer1");
            User seller = createTestSeller("seller1");
            Order order = createCompletedOrder(buyer, seller);
            
            Review review = reviewService.createReview(buyer, order.getOrderId(), 5, "Excellent product!");
            
            assert review != null : "Review should not be null";
            assert review.getRating() == 5 : "Rating should be 5";
            assert "Excellent product!".equals(review.getContent()) : "Content should match";
            assert buyer.getUserId().equals(review.getReviewerId()) : "Reviewer ID should match";
            assert seller.getUserId().equals(review.getRevieweeId()) : "Reviewee ID should match";
            
            System.out.println("[PASS] testCreateReview_Success");
        } catch (Exception e) {
            System.out.println("[FAIL] testCreateReview_Success: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testCreateReview_NotBuyer_ThrowsException() {
        setUp();
        try {
            User seller1 = createTestSeller("seller1");
            User seller2 = createTestSeller("seller2");
            
            Product product = productService.publishProduct(
                seller1, "iPhone 13", "Like new", 
                3999.99, ProductCategory.ELECTRONICS, ProductCondition.LIKE_NEW
            );
            
            try {
                reviewService.createReview(seller2, "O001", 5, "Good product");
                System.out.println("[FAIL] testCreateReview_NotBuyer: Should throw PermissionDeniedException");
            } catch (PermissionDeniedException e) {
                System.out.println("[PASS] testCreateReview_NotBuyer_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testCreateReview_NotBuyer: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testCreateReview_BannedUser_ThrowsException() {
        setUp();
        try {
            User buyer = createTestBuyer("buyer1");
            User seller = createTestSeller("seller1");
            Order order = createCompletedOrder(buyer, seller);
            
            // Ban the buyer
            buyer.setStatus(enums.UserStatus.BANNED);
            
            try {
                reviewService.createReview(buyer, order.getOrderId(), 5, "Good product");
                System.out.println("[FAIL] testCreateReview_BannedUser: Should throw PermissionDeniedException");
            } catch (PermissionDeniedException e) {
                System.out.println("[PASS] testCreateReview_BannedUser_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testCreateReview_BannedUser: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testCreateReview_NotOwner_ThrowsException() {
        setUp();
        try {
            User buyer1 = createTestBuyer("buyer1");
            User buyer2 = createTestBuyer("buyer2");
            User seller = createTestSeller("seller1");
            
            Order order = createCompletedOrder(buyer1, seller);
            
            try {
                reviewService.createReview(buyer2, order.getOrderId(), 5, "Good product");
                System.out.println("[FAIL] testCreateReview_NotOwner: Should throw PermissionDeniedException");
            } catch (PermissionDeniedException e) {
                System.out.println("[PASS] testCreateReview_NotOwner_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testCreateReview_NotOwner: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testCreateReview_OrderNotCompleted_ThrowsException() {
        setUp();
        try {
            User buyer = createTestBuyer("buyer1");
            User seller = createTestSeller("seller1");
            
            Product product = productService.publishProduct(
                seller, "iPhone 13", "Like new", 
                3999.99, ProductCategory.ELECTRONICS, ProductCondition.LIKE_NEW
            );
            
            Order order = orderService.createOrder(buyer, product.getProductId());
            
            try {
                reviewService.createReview(buyer, order.getOrderId(), 5, "Good product");
                System.out.println("[FAIL] testCreateReview_OrderNotCompleted: Should throw BusinessException");
            } catch (BusinessException e) {
                System.out.println("[PASS] testCreateReview_OrderNotCompleted_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testCreateReview_OrderNotCompleted: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testCreateReview_AlreadyReviewed_ThrowsException() {
        setUp();
        try {
            User buyer = createTestBuyer("buyer1");
            User seller = createTestSeller("seller1");
            Order order = createCompletedOrder(buyer, seller);
            
            // First review
            reviewService.createReview(buyer, order.getOrderId(), 5, "Good product");
            
            try {
                // Try to review again
                reviewService.createReview(buyer, order.getOrderId(), 4, "Another review");
                System.out.println("[FAIL] testCreateReview_AlreadyReviewed: Should throw BusinessException");
            } catch (BusinessException e) {
                System.out.println("[PASS] testCreateReview_AlreadyReviewed_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testCreateReview_AlreadyReviewed: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testCreateReview_InvalidRating_TooLow_ThrowsException() {
        setUp();
        try {
            User buyer = createTestBuyer("buyer1");
            User seller = createTestSeller("seller1");
            Order order = createCompletedOrder(buyer, seller);
            
            try {
                reviewService.createReview(buyer, order.getOrderId(), 0, "Bad rating");
                System.out.println("[FAIL] testCreateReview_InvalidRating_TooLow: Should throw BusinessException");
            } catch (BusinessException e) {
                System.out.println("[PASS] testCreateReview_InvalidRating_TooLow_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testCreateReview_InvalidRating_TooLow: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testCreateReview_InvalidRating_TooHigh_ThrowsException() {
        setUp();
        try {
            User buyer = createTestBuyer("buyer1");
            User seller = createTestSeller("seller1");
            Order order = createCompletedOrder(buyer, seller);
            
            try {
                reviewService.createReview(buyer, order.getOrderId(), 6, "Too high");
                System.out.println("[FAIL] testCreateReview_InvalidRating_TooHigh: Should throw BusinessException");
            } catch (BusinessException e) {
                System.out.println("[PASS] testCreateReview_InvalidRating_TooHigh_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testCreateReview_InvalidRating_TooHigh: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    // ========== Reputation Update Tests ==========
    
    public void testCreateReview_5Star_ReputationIncrease5() {
        setUp();
        try {
            User buyer = createTestBuyer("buyer1");
            User seller = createTestSeller("seller1");
            Order order = createCompletedOrder(buyer, seller);
            
            int sellerRepBefore = seller.getReputation();
            
            reviewService.createReview(buyer, order.getOrderId(), 5, "Excellent product!");
            
            assert seller.getReputation() == sellerRepBefore + 5 : "Seller reputation should increase by 5";
            
            System.out.println("[PASS] testCreateReview_5Star_ReputationIncrease5");
        } catch (Exception e) {
            System.out.println("[FAIL] testCreateReview_5Star: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testCreateReview_4Star_ReputationIncrease2() {
        setUp();
        try {
            User buyer = createTestBuyer("buyer1");
            User seller = createTestSeller("seller1");
            Order order = createCompletedOrder(buyer, seller);
            
            int sellerRepBefore = seller.getReputation();
            
            reviewService.createReview(buyer, order.getOrderId(), 4, "Good product");
            
            assert seller.getReputation() == sellerRepBefore + 2 : "Seller reputation should increase by 2";
            
            System.out.println("[PASS] testCreateReview_4Star_ReputationIncrease2");
        } catch (Exception e) {
            System.out.println("[FAIL] testCreateReview_4Star: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testCreateReview_3Star_ReputationNoChange() {
        setUp();
        try {
            User buyer = createTestBuyer("buyer1");
            User seller = createTestSeller("seller1");
            Order order = createCompletedOrder(buyer, seller);
            
            int sellerRepBefore = seller.getReputation();
            
            reviewService.createReview(buyer, order.getOrderId(), 3, "Average product");
            
            assert seller.getReputation() == sellerRepBefore : "Seller reputation should not change";
            
            System.out.println("[PASS] testCreateReview_3Star_ReputationNoChange");
        } catch (Exception e) {
            System.out.println("[FAIL] testCreateReview_3Star: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testCreateReview_2Star_ReputationDecrease2() {
        setUp();
        try {
            User buyer = createTestBuyer("buyer1");
            User seller = createTestSeller("seller1");
            Order order = createCompletedOrder(buyer, seller);
            
            int sellerRepBefore = seller.getReputation();
            
            reviewService.createReview(buyer, order.getOrderId(), 2, "Below average");
            
            assert seller.getReputation() == sellerRepBefore - 2 : "Seller reputation should decrease by 2";
            
            System.out.println("[PASS] testCreateReview_2Star_ReputationDecrease2");
        } catch (Exception e) {
            System.out.println("[FAIL] testCreateReview_2Star: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testCreateReview_1Star_ReputationDecrease3() {
        setUp();
        try {
            User buyer = createTestBuyer("buyer1");
            User seller = createTestSeller("seller1");
            Order order = createCompletedOrder(buyer, seller);
            
            int sellerRepBefore = seller.getReputation();
            
            reviewService.createReview(buyer, order.getOrderId(), 1, "Very bad product");
            
            assert seller.getReputation() == sellerRepBefore - 3 : "Seller reputation should decrease by 3";
            
            System.out.println("[PASS] testCreateReview_1Star_ReputationDecrease3");
        } catch (Exception e) {
            System.out.println("[FAIL] testCreateReview_1Star: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    // ========== Query Tests ==========
    
    public void testGetReviewById_Success() {
        setUp();
        try {
            User buyer = createTestBuyer("buyer1");
            User seller = createTestSeller("seller1");
            Order order = createCompletedOrder(buyer, seller);
            
            Review created = reviewService.createReview(buyer, order.getOrderId(), 5, "Good!");
            Review retrieved = reviewService.getReviewById(created.getReviewId());
            
            assert retrieved != null : "Retrieved review should not be null";
            assert created.getReviewId().equals(retrieved.getReviewId()) : "Review IDs should match";
            
            System.out.println("[PASS] testGetReviewById_Success");
        } catch (Exception e) {
            System.out.println("[FAIL] testGetReviewById_Success: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testGetReviewById_NotExists_ThrowsException() {
        setUp();
        try {
            try {
                reviewService.getReviewById("R999");
                System.out.println("[FAIL] testGetReviewById_NotExists: Should throw ResourceNotFoundException");
            } catch (ResourceNotFoundException e) {
                System.out.println("[PASS] testGetReviewById_NotExists_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testGetReviewById_NotExists: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testGetReviewByOrderId_Success() {
        setUp();
        try {
            User buyer = createTestBuyer("buyer1");
            User seller = createTestSeller("seller1");
            Order order = createCompletedOrder(buyer, seller);
            
            reviewService.createReview(buyer, order.getOrderId(), 5, "Good!");
            Review retrieved = reviewService.getReviewByOrderId(order.getOrderId());
            
            assert retrieved != null : "Retrieved review should not be null";
            assert order.getOrderId().equals(retrieved.getOrderId()) : "Order IDs should match";
            
            System.out.println("[PASS] testGetReviewByOrderId_Success");
        } catch (Exception e) {
            System.out.println("[FAIL] testGetReviewByOrderId_Success: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testGetReviewsBySeller_MultipleReviews() {
        setUp();
        try {
            User buyer1 = createTestBuyer("buyer1");
            User buyer2 = createTestBuyer("buyer2");
            User seller = createTestSeller("seller1");
            
            Order order1 = createCompletedOrder(buyer1, seller);
            Order order2 = createCompletedOrder(buyer2, seller);
            
            reviewService.createReview(buyer1, order1.getOrderId(), 5, "Good!");
            reviewService.createReview(buyer2, order2.getOrderId(), 4, "Nice!");
            
            List<Review> reviews = reviewService.getReviewsBySeller(seller.getUserId());
            
            assert reviews.size() == 2 : "Should have 2 reviews";
            
            System.out.println("[PASS] testGetReviewsBySeller_MultipleReviews");
        } catch (Exception e) {
            System.out.println("[FAIL] testGetReviewsBySeller: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testGetAverageRating_Success() {
        setUp();
        try {
            User buyer1 = createTestBuyer("buyer1");
            User buyer2 = createTestBuyer("buyer2");
            User seller = createTestSeller("seller1");
            
            Order order1 = createCompletedOrder(buyer1, seller);
            Order order2 = createCompletedOrder(buyer2, seller);
            
            reviewService.createReview(buyer1, order1.getOrderId(), 5, "Good!");
            reviewService.createReview(buyer2, order2.getOrderId(), 3, "Average");
            
            double avgRating = reviewService.getAverageRating(seller.getUserId());
            
            assert avgRating == 4.0 : "Average rating should be 4.0";
            
            System.out.println("[PASS] testGetAverageRating_Success");
        } catch (Exception e) {
            System.out.println("[FAIL] testGetAverageRating: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testGetAverageRating_NoReviews_ReturnsZero() {
        setUp();
        try {
            User seller = createTestSeller("seller1");
            
            double avgRating = reviewService.getAverageRating(seller.getUserId());
            
            assert avgRating == 0.0 : "Average rating should be 0.0 when no reviews";
            
            System.out.println("[PASS] testGetAverageRating_NoReviews_ReturnsZero");
        } catch (Exception e) {
            System.out.println("[FAIL] testGetAverageRating_NoReviews: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    // ========== Main Method ==========
    
    public static void main(String[] args) {
        ReviewServiceTest test = new ReviewServiceTest();
        
        System.out.println("========================================");
        System.out.println("Running ReviewService Unit Tests");
        System.out.println("========================================\n");
        
        // Create Review Tests
        test.testCreateReview_Success();
        test.testCreateReview_NotBuyer_ThrowsException();
        test.testCreateReview_BannedUser_ThrowsException();
        test.testCreateReview_NotOwner_ThrowsException();
        test.testCreateReview_OrderNotCompleted_ThrowsException();
        test.testCreateReview_AlreadyReviewed_ThrowsException();
        test.testCreateReview_InvalidRating_TooLow_ThrowsException();
        test.testCreateReview_InvalidRating_TooHigh_ThrowsException();
        
        // Reputation Update Tests
        test.testCreateReview_5Star_ReputationIncrease5();
        test.testCreateReview_4Star_ReputationIncrease2();
        test.testCreateReview_3Star_ReputationNoChange();
        test.testCreateReview_2Star_ReputationDecrease2();
        test.testCreateReview_1Star_ReputationDecrease3();
        
        // Query Tests
        test.testGetReviewById_Success();
        test.testGetReviewById_NotExists_ThrowsException();
        test.testGetReviewByOrderId_Success();
        test.testGetReviewsBySeller_MultipleReviews();
        test.testGetAverageRating_Success();
        test.testGetAverageRating_NoReviews_ReturnsZero();
        
        System.out.println("\n========================================");
        System.out.println("All ReviewService Tests Completed");
        System.out.println("========================================");
    }
}

