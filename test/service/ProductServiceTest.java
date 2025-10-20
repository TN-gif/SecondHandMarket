package service;

import entity.Product;
import entity.User;
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
 * ProductService Unit Tests
 * 
 * Test Coverage:
 * 1. Publish Product (Success / Failure scenarios)
 * 2. Edit Product
 * 3. Remove Product
 * 4. Re-list Product
 */
public class ProductServiceTest {
    
    private ProductService productService;
    private UserService userService;
    private NotificationService notificationService;
    
    public void setUp() {
        DataCenter.getInstance().clearAll();
        
        notificationService = new NotificationService();
        userService = new UserService(notificationService);
        productService = new ProductService();
        
        System.out.println("[TEST] Setup completed");
    }
    
    public void tearDown() {
        DataCenter.getInstance().clearAll();
        System.out.println("[TEST] Teardown completed");
    }
    
    /**
     * Helper: Create a test seller
     */
    private User createTestSeller(String username) {
        Set<UserRole> roles = EnumSet.of(UserRole.SELLER);
        return userService.register(username, "password123", roles);
    }
    
    /**
     * Helper: Create a test buyer
     */
    private User createTestBuyer(String username) {
        Set<UserRole> roles = EnumSet.of(UserRole.BUYER);
        return userService.register(username, "password123", roles);
    }
    
    // ========== Publish Product Tests ==========
    
    public void testPublishProduct_ValidInput_Success() {
        setUp();
        try {
            User seller = createTestSeller("seller1");
            
            Product product = productService.publishProduct(
                seller, "iPhone 13", "Like new condition", 
                3999.99, ProductCategory.ELECTRONICS, ProductCondition.LIKE_NEW
            );
            
            assert product != null : "Product should not be null";
            assert "iPhone 13".equals(product.getTitle()) : "Title should match";
            assert product.getPrice() == 3999.99 : "Price should match";
            assert product.getStatus() == ProductStatus.AVAILABLE : "Status should be AVAILABLE";
            assert seller.getUserId().equals(product.getSellerId()) : "Seller ID should match";
            
            System.out.println("[PASS] testPublishProduct_ValidInput_Success");
        } catch (Exception e) {
            System.out.println("[FAIL] testPublishProduct_ValidInput: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testPublishProduct_NotSeller_ThrowsException() {
        setUp();
        try {
            User buyer = createTestBuyer("buyer1");
            
            try {
                productService.publishProduct(
                    buyer, "iPhone 13", "Description", 
                    3999.99, ProductCategory.ELECTRONICS, ProductCondition.LIKE_NEW
                );
                System.out.println("[FAIL] testPublishProduct_NotSeller: Should throw PermissionDeniedException");
            } catch (PermissionDeniedException e) {
                System.out.println("[PASS] testPublishProduct_NotSeller_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testPublishProduct_NotSeller: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testPublishProduct_InvalidPrice_ThrowsException() {
        setUp();
        try {
            User seller = createTestSeller("seller1");
            
            try {
                productService.publishProduct(
                    seller, "iPhone 13", "Description", 
                    -100.0, ProductCategory.ELECTRONICS, ProductCondition.LIKE_NEW
                );
                System.out.println("[FAIL] testPublishProduct_InvalidPrice: Should throw BusinessException");
            } catch (BusinessException e) {
                System.out.println("[PASS] testPublishProduct_InvalidPrice_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testPublishProduct_InvalidPrice: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testPublishProduct_EmptyTitle_ThrowsException() {
        setUp();
        try {
            User seller = createTestSeller("seller1");
            
            try {
                productService.publishProduct(
                    seller, "", "Description", 
                    3999.99, ProductCategory.ELECTRONICS, ProductCondition.LIKE_NEW
                );
                System.out.println("[FAIL] testPublishProduct_EmptyTitle: Should throw BusinessException");
            } catch (BusinessException e) {
                System.out.println("[PASS] testPublishProduct_EmptyTitle_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testPublishProduct_EmptyTitle: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    // ========== Edit Product Tests ==========
    
    public void testEditProduct_Success() {
        setUp();
        try {
            User seller = createTestSeller("seller1");
            
            Product product = productService.publishProduct(
                seller, "iPhone 13", "Description", 
                3999.99, ProductCategory.ELECTRONICS, ProductCondition.LIKE_NEW
            );
            
            // Edit product
            productService.editProduct(seller, product.getProductId(), 
                "iPhone 13 Pro", "Updated description", 4999.99);
            
            assert "iPhone 13 Pro".equals(product.getTitle()) : "Title should be updated";
            assert product.getPrice() == 4999.99 : "Price should be updated";
            assert "Updated description".equals(product.getDescription()) : "Description should be updated";
            
            System.out.println("[PASS] testEditProduct_Success");
        } catch (Exception e) {
            System.out.println("[FAIL] testEditProduct: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testEditProduct_NotOwner_ThrowsException() {
        setUp();
        try {
            User seller1 = createTestSeller("seller1");
            User seller2 = createTestSeller("seller2");
            
            Product product = productService.publishProduct(
                seller1, "iPhone 13", "Description", 
                3999.99, ProductCategory.ELECTRONICS, ProductCondition.LIKE_NEW
            );
            
            try {
                productService.editProduct(seller2, product.getProductId(), 
                    "New title", "New description", 5000.0);
                System.out.println("[FAIL] testEditProduct_NotOwner: Should throw PermissionDeniedException");
            } catch (PermissionDeniedException e) {
                System.out.println("[PASS] testEditProduct_NotOwner_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testEditProduct_NotOwner: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    // ========== Remove Product Tests ==========
    
    public void testRemoveProduct_Success() {
        setUp();
        try {
            User seller = createTestSeller("seller1");
            
            Product product = productService.publishProduct(
                seller, "iPhone 13", "Description", 
                3999.99, ProductCategory.ELECTRONICS, ProductCondition.LIKE_NEW
            );
            
            assert product.getStatus() == ProductStatus.AVAILABLE : "Initial status should be AVAILABLE";
            
            // Remove product
            productService.removeProduct(seller, product.getProductId());
            
            assert product.getStatus() == ProductStatus.REMOVED : "Status should be REMOVED";
            
            System.out.println("[PASS] testRemoveProduct_Success");
        } catch (Exception e) {
            System.out.println("[FAIL] testRemoveProduct: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testReListProduct_Success() {
        setUp();
        try {
            User seller = createTestSeller("seller1");
            
            Product product = productService.publishProduct(
                seller, "iPhone 13", "Description", 
                3999.99, ProductCategory.ELECTRONICS, ProductCondition.LIKE_NEW
            );
            
            // Remove then re-list
            productService.removeProduct(seller, product.getProductId());
            assert product.getStatus() == ProductStatus.REMOVED : "Status should be REMOVED";
            
            productService.reListProduct(seller, product.getProductId());
            assert product.getStatus() == ProductStatus.AVAILABLE : "Status should be AVAILABLE again";
            
            System.out.println("[PASS] testReListProduct_Success");
        } catch (Exception e) {
            System.out.println("[FAIL] testReListProduct: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    // ========== User Status Tests ==========
    
    public void testPublishProduct_BannedUser_ThrowsException() {
        setUp();
        try {
            User seller = createTestSeller("seller1");
            seller.setStatus(enums.UserStatus.BANNED);
            
            try {
                productService.publishProduct(
                    seller, "iPhone 13", "Description", 
                    3999.99, ProductCategory.ELECTRONICS, ProductCondition.LIKE_NEW
                );
                System.out.println("[FAIL] testPublishProduct_BannedUser: Should throw PermissionDeniedException");
            } catch (PermissionDeniedException e) {
                System.out.println("[PASS] testPublishProduct_BannedUser_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testPublishProduct_BannedUser: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testPublishProduct_PriceWithThreeDecimals_ThrowsException() {
        setUp();
        try {
            User seller = createTestSeller("seller1");
            
            try {
                productService.publishProduct(
                    seller, "iPhone 13", "Description", 
                    3999.999, ProductCategory.ELECTRONICS, ProductCondition.LIKE_NEW
                );
                System.out.println("[FAIL] testPublishProduct_PriceWithThreeDecimals: Should throw BusinessException");
            } catch (BusinessException e) {
                System.out.println("[PASS] testPublishProduct_PriceWithThreeDecimals_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testPublishProduct_PriceWithThreeDecimals: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    // ========== Validation Tests ==========
    
    public void testPublishProduct_PriceTooLarge_ThrowsException() {
        setUp();
        try {
            User seller = createTestSeller("seller1");
            
            try {
                productService.publishProduct(
                    seller, "Expensive Item", "Description", 
                    2000000.0, ProductCategory.ELECTRONICS, ProductCondition.LIKE_NEW
                );
                System.out.println("[FAIL] testPublishProduct_PriceTooLarge: Should throw BusinessException");
            } catch (BusinessException e) {
                System.out.println("[PASS] testPublishProduct_PriceTooLarge_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testPublishProduct_PriceTooLarge: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testPublishProduct_TitleTooShort_ThrowsException() {
        setUp();
        try {
            User seller = createTestSeller("seller1");
            
            try {
                productService.publishProduct(
                    seller, "A", "Description", 
                    3999.99, ProductCategory.ELECTRONICS, ProductCondition.LIKE_NEW
                );
                System.out.println("[FAIL] testPublishProduct_TitleTooShort: Should throw BusinessException");
            } catch (BusinessException e) {
                System.out.println("[PASS] testPublishProduct_TitleTooShort_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testPublishProduct_TitleTooShort: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testPublishProduct_TitleTooLong_ThrowsException() {
        setUp();
        try {
            User seller = createTestSeller("seller1");
            String longTitle = "A".repeat(101);
            
            try {
                productService.publishProduct(
                    seller, longTitle, "Description", 
                    3999.99, ProductCategory.ELECTRONICS, ProductCondition.LIKE_NEW
                );
                System.out.println("[FAIL] testPublishProduct_TitleTooLong: Should throw BusinessException");
            } catch (BusinessException e) {
                System.out.println("[PASS] testPublishProduct_TitleTooLong_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testPublishProduct_TitleTooLong: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testPublishProduct_DescriptionTooLong_ThrowsException() {
        setUp();
        try {
            User seller = createTestSeller("seller1");
            String longDesc = "A".repeat(1001);
            
            try {
                productService.publishProduct(
                    seller, "iPhone 13", longDesc, 
                    3999.99, ProductCategory.ELECTRONICS, ProductCondition.LIKE_NEW
                );
                System.out.println("[FAIL] testPublishProduct_DescriptionTooLong: Should throw BusinessException");
            } catch (BusinessException e) {
                System.out.println("[PASS] testPublishProduct_DescriptionTooLong_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testPublishProduct_DescriptionTooLong: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    // ========== Edit Product Edge Cases ==========
    
    public void testEditProduct_InvalidPrice_ThrowsException() {
        setUp();
        try {
            User seller = createTestSeller("seller1");
            Product product = productService.publishProduct(
                seller, "iPhone 13", "Description", 
                3999.99, ProductCategory.ELECTRONICS, ProductCondition.LIKE_NEW
            );
            
            try {
                productService.editProduct(seller, product.getProductId(), 
                    "iPhone 13 Pro", "Updated", -100.0);
                System.out.println("[FAIL] testEditProduct_InvalidPrice: Should throw BusinessException");
            } catch (BusinessException e) {
                System.out.println("[PASS] testEditProduct_InvalidPrice_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testEditProduct_InvalidPrice: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testEditProduct_NotExists_ThrowsException() {
        setUp();
        try {
            User seller = createTestSeller("seller1");
            
            try {
                productService.editProduct(seller, "P999", 
                    "New title", "New description", 5000.0);
                System.out.println("[FAIL] testEditProduct_NotExists: Should throw exception");
            } catch (Exception e) {
                System.out.println("[PASS] testEditProduct_NotExists_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testEditProduct_NotExists: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    // ========== Remove Product Edge Cases ==========
    
    public void testRemoveProduct_NotOwner_ThrowsException() {
        setUp();
        try {
            User seller1 = createTestSeller("seller1");
            User seller2 = createTestSeller("seller2");
            
            Product product = productService.publishProduct(
                seller1, "iPhone 13", "Description", 
                3999.99, ProductCategory.ELECTRONICS, ProductCondition.LIKE_NEW
            );
            
            try {
                productService.removeProduct(seller2, product.getProductId());
                System.out.println("[FAIL] testRemoveProduct_NotOwner: Should throw PermissionDeniedException");
            } catch (PermissionDeniedException e) {
                System.out.println("[PASS] testRemoveProduct_NotOwner_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testRemoveProduct_NotOwner: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    // ========== Query Tests ==========
    
    public void testGetProductById_NotExists_ThrowsException() {
        setUp();
        try {
            try {
                productService.getProductById("P999");
                System.out.println("[FAIL] testGetProductById_NotExists: Should throw exception");
            } catch (Exception e) {
                System.out.println("[PASS] testGetProductById_NotExists_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testGetProductById_NotExists: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    public void testGetProductsBySeller_Success() {
        setUp();
        try {
            User seller = createTestSeller("seller1");
            
            productService.publishProduct(
                seller, "iPhone 13", "Description1", 
                3999.99, ProductCategory.ELECTRONICS, ProductCondition.LIKE_NEW
            );
            productService.publishProduct(
                seller, "MacBook Pro", "Description2", 
                8999.99, ProductCategory.ELECTRONICS, ProductCondition.BRAND_NEW
            );
            
            java.util.List<Product> products = productService.getProductsBySeller(seller.getUserId());
            
            assert products.size() == 2 : "Should have 2 products";
            
            System.out.println("[PASS] testGetProductsBySeller_Success");
        } catch (Exception e) {
            System.out.println("[FAIL] testGetProductsBySeller: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    // ========== Main Method ==========
    
    public static void main(String[] args) {
        ProductServiceTest test = new ProductServiceTest();
        
        System.out.println("========================================");
        System.out.println("Running ProductService Unit Tests");
        System.out.println("========================================\n");
        
        // Basic Tests
        test.testPublishProduct_ValidInput_Success();
        test.testPublishProduct_NotSeller_ThrowsException();
        test.testPublishProduct_InvalidPrice_ThrowsException();
        test.testPublishProduct_EmptyTitle_ThrowsException();
        
        // User Status Tests
        test.testPublishProduct_BannedUser_ThrowsException();
        test.testPublishProduct_PriceWithThreeDecimals_ThrowsException();
        
        // Validation Tests
        test.testPublishProduct_PriceTooLarge_ThrowsException();
        test.testPublishProduct_TitleTooShort_ThrowsException();
        test.testPublishProduct_TitleTooLong_ThrowsException();
        test.testPublishProduct_DescriptionTooLong_ThrowsException();
        
        // Edit Tests
        test.testEditProduct_Success();
        test.testEditProduct_NotOwner_ThrowsException();
        test.testEditProduct_InvalidPrice_ThrowsException();
        test.testEditProduct_NotExists_ThrowsException();
        
        // Remove/ReList Tests
        test.testRemoveProduct_Success();
        test.testRemoveProduct_NotOwner_ThrowsException();
        test.testReListProduct_Success();
        
        // Query Tests
        test.testGetProductById_NotExists_ThrowsException();
        test.testGetProductsBySeller_Success();
        
        System.out.println("\n========================================");
        System.out.println("All ProductService Tests Completed");
        System.out.println("========================================");
    }
}

