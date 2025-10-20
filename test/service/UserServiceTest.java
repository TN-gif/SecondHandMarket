package service;

import entity.User;
import enums.UserRole;
import enums.UserStatus;
import exception.AuthenticationException;
import exception.BusinessException;
import repository.DataCenter;

import java.util.EnumSet;
import java.util.Set;

/**
 * UserService Unit Tests
 * 
 * Test Coverage:
 * 1. User Registration (Success / Failure scenarios)
 * 2. User Login (Success / Failure scenarios)
 * 3. User Logout
 * 4. Password Change
 * 5. Role Validation
 */
public class UserServiceTest {
    
    private UserService userService;
    private NotificationService notificationService;
    
    /**
     * Setup method - called before each test
     */
    public void setUp() {
        // Clear all data to ensure test independence
        DataCenter.getInstance().clearAll();
        
        // Create service instances
        notificationService = new NotificationService();
        userService = new UserService(notificationService);
        
        System.out.println("[TEST] Setup completed");
    }
    
    /**
     * Teardown method - called after each test
     */
    public void tearDown() {
        DataCenter.getInstance().clearAll();
        System.out.println("[TEST] Teardown completed");
    }
    
    // ========== Registration Tests ==========
    
    /**
     * Test: Register with valid input
     * Expected: User created successfully
     */
    public void testRegister_ValidInput_Success() {
        setUp();
        try {
            Set<UserRole> roles = EnumSet.of(UserRole.BUYER);
            User user = userService.register("testuser", "password123", roles);
            
            assert user != null : "User should not be null";
            assert "testuser".equals(user.getUsername()) : "Username should match";
            assert user.hasRole(UserRole.BUYER) : "User should have BUYER role";
            assert user.getReputation() == 100 : "Initial reputation should be 100";
            assert user.getStatus() == UserStatus.ACTIVE : "Initial status should be ACTIVE";
            
            System.out.println("[PASS] testRegister_ValidInput_Success");
        } catch (Exception e) {
            System.out.println("[FAIL] testRegister_ValidInput_Success: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    /**
     * Test: Register with duplicate username
     * Expected: BusinessException thrown
     */
    public void testRegister_DuplicateUsername_ThrowsException() {
        setUp();
        try {
            Set<UserRole> roles = EnumSet.of(UserRole.BUYER);
            userService.register("testuser", "password123", roles);
            
            // Try to register again with same username
            try {
                userService.register("testuser", "password456", roles);
                System.out.println("[FAIL] testRegister_DuplicateUsername: Should throw BusinessException");
            } catch (BusinessException e) {
                assert e.getMessage().contains("already exists") : "Error message should indicate duplicate";
                System.out.println("[PASS] testRegister_DuplicateUsername_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testRegister_DuplicateUsername: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    /**
     * Test: Register with invalid username (too short)
     * Expected: BusinessException thrown
     */
    public void testRegister_UsernameTooShort_ThrowsException() {
        setUp();
        try {
            Set<UserRole> roles = EnumSet.of(UserRole.BUYER);
            
            try {
                userService.register("abc", "password123", roles);
                System.out.println("[FAIL] testRegister_UsernameTooShort: Should throw BusinessException");
            } catch (BusinessException e) {
                System.out.println("[PASS] testRegister_UsernameTooShort_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testRegister_UsernameTooShort: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    /**
     * Test: Register with invalid password (too short)
     * Expected: BusinessException thrown
     */
    public void testRegister_PasswordTooShort_ThrowsException() {
        setUp();
        try {
            Set<UserRole> roles = EnumSet.of(UserRole.BUYER);
            
            try {
                userService.register("testuser", "12345", roles);
                System.out.println("[FAIL] testRegister_PasswordTooShort: Should throw BusinessException");
            } catch (BusinessException e) {
                System.out.println("[PASS] testRegister_PasswordTooShort_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testRegister_PasswordTooShort: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    /**
     * Test: Register with empty roles
     * Expected: BusinessException thrown
     */
    public void testRegister_EmptyRoles_ThrowsException() {
        setUp();
        try {
            Set<UserRole> roles = EnumSet.noneOf(UserRole.class);
            
            try {
                userService.register("testuser", "password123", roles);
                System.out.println("[FAIL] testRegister_EmptyRoles: Should throw BusinessException");
            } catch (BusinessException e) {
                System.out.println("[PASS] testRegister_EmptyRoles_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testRegister_EmptyRoles: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    /**
     * Test: Register with multiple roles
     * Expected: User created with both roles
     */
    public void testRegister_MultipleRoles_Success() {
        setUp();
        try {
            Set<UserRole> roles = EnumSet.of(UserRole.BUYER, UserRole.SELLER);
            User user = userService.register("testuser", "password123", roles);
            
            assert user.hasRole(UserRole.BUYER) : "Should have BUYER role";
            assert user.hasRole(UserRole.SELLER) : "Should have SELLER role";
            
            System.out.println("[PASS] testRegister_MultipleRoles_Success");
        } catch (Exception e) {
            System.out.println("[FAIL] testRegister_MultipleRoles: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    // ========== Login Tests ==========
    
    /**
     * Test: Login with valid credentials
     * Expected: Login successful
     */
    public void testLogin_ValidCredentials_Success() {
        setUp();
        try {
            // Register a user first
            Set<UserRole> roles = EnumSet.of(UserRole.BUYER);
            userService.register("testuser", "password123", roles);
            
            // Login
            User user = userService.login("testuser", "password123");
            
            assert user != null : "User should not be null";
            assert "testuser".equals(user.getUsername()) : "Username should match";
            assert userService.isLoggedIn() : "Should be logged in";
            assert user.equals(userService.getCurrentUser()) : "Current user should match";
            
            System.out.println("[PASS] testLogin_ValidCredentials_Success");
        } catch (Exception e) {
            System.out.println("[FAIL] testLogin_ValidCredentials: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    /**
     * Test: Login with non-existent username
     * Expected: AuthenticationException thrown
     */
    public void testLogin_UsernameNotExists_ThrowsException() {
        setUp();
        try {
            try {
                userService.login("nonexistent", "password123");
                System.out.println("[FAIL] testLogin_UsernameNotExists: Should throw AuthenticationException");
            } catch (AuthenticationException e) {
                System.out.println("[PASS] testLogin_UsernameNotExists_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testLogin_UsernameNotExists: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    /**
     * Test: Login with wrong password
     * Expected: AuthenticationException thrown
     */
    public void testLogin_WrongPassword_ThrowsException() {
        setUp();
        try {
            Set<UserRole> roles = EnumSet.of(UserRole.BUYER);
            userService.register("testuser", "password123", roles);
            
            try {
                userService.login("testuser", "wrongpassword");
                System.out.println("[FAIL] testLogin_WrongPassword: Should throw AuthenticationException");
            } catch (AuthenticationException e) {
                System.out.println("[PASS] testLogin_WrongPassword_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testLogin_WrongPassword: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    /**
     * Test: Logout
     * Expected: User logged out successfully
     */
    public void testLogout_Success() {
        setUp();
        try {
            Set<UserRole> roles = EnumSet.of(UserRole.BUYER);
            userService.register("testuser", "password123", roles);
            userService.login("testuser", "password123");
            
            assert userService.isLoggedIn() : "Should be logged in";
            
            userService.logout();
            
            assert !userService.isLoggedIn() : "Should not be logged in after logout";
            
            System.out.println("[PASS] testLogout_Success");
        } catch (Exception e) {
            System.out.println("[FAIL] testLogout: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    // ========== Password Change Tests ==========
    
    /**
     * Test: Change password successfully
     * Expected: Password changed, can login with new password
     */
    public void testChangePassword_Success() {
        setUp();
        try {
            Set<UserRole> roles = EnumSet.of(UserRole.BUYER);
            userService.register("testuser", "oldpassword", roles);
            userService.login("testuser", "oldpassword");
            
            // Change password
            userService.changePassword("oldpassword", "newpassword");
            
            // Logout and login with new password
            userService.logout();
            User user = userService.login("testuser", "newpassword");
            
            assert user != null : "Should be able to login with new password";
            
            System.out.println("[PASS] testChangePassword_Success");
        } catch (Exception e) {
            System.out.println("[FAIL] testChangePassword: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    /**
     * Test: Change password with wrong old password
     * Expected: BusinessException thrown
     */
    public void testChangePassword_WrongOldPassword_ThrowsException() {
        setUp();
        try {
            Set<UserRole> roles = EnumSet.of(UserRole.BUYER);
            userService.register("testuser", "oldpassword", roles);
            userService.login("testuser", "oldpassword");
            
            try {
                userService.changePassword("wrongold", "newpassword");
                System.out.println("[FAIL] testChangePassword_WrongOldPassword: Should throw BusinessException");
            } catch (BusinessException e) {
                System.out.println("[PASS] testChangePassword_WrongOldPassword_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testChangePassword_WrongOldPassword: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    // ========== Edge Case Tests ==========
    
    /**
     * Test: Register with username too long (>20 chars)
     * Expected: BusinessException thrown
     */
    public void testRegister_UsernameTooLong_ThrowsException() {
        setUp();
        try {
            Set<UserRole> roles = EnumSet.of(UserRole.BUYER);
            
            try {
                userService.register("thisusernameiswaytoolong", "password123", roles);
                System.out.println("[FAIL] testRegister_UsernameTooLong: Should throw BusinessException");
            } catch (BusinessException e) {
                System.out.println("[PASS] testRegister_UsernameTooLong_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testRegister_UsernameTooLong: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    /**
     * Test: Register with username containing special characters
     * Expected: BusinessException thrown
     */
    public void testRegister_UsernameInvalidChars_ThrowsException() {
        setUp();
        try {
            Set<UserRole> roles = EnumSet.of(UserRole.BUYER);
            
            try {
                userService.register("user@123", "password123", roles);
                System.out.println("[FAIL] testRegister_UsernameInvalidChars: Should throw BusinessException");
            } catch (BusinessException e) {
                System.out.println("[PASS] testRegister_UsernameInvalidChars_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testRegister_UsernameInvalidChars: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    /**
     * Test: Register with password too long (>20 chars)
     * Expected: BusinessException thrown
     */
    public void testRegister_PasswordTooLong_ThrowsException() {
        setUp();
        try {
            Set<UserRole> roles = EnumSet.of(UserRole.BUYER);
            
            try {
                userService.register("testuser", "thispasswordiswaytoolongandexceeds", roles);
                System.out.println("[FAIL] testRegister_PasswordTooLong: Should throw BusinessException");
            } catch (BusinessException e) {
                System.out.println("[PASS] testRegister_PasswordTooLong_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testRegister_PasswordTooLong: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    /**
     * Test: Change password when not logged in
     * Expected: AuthenticationException thrown
     */
    public void testChangePassword_NotLoggedIn_ThrowsException() {
        setUp();
        try {
            try {
                userService.changePassword("oldpass", "newpass");
                System.out.println("[FAIL] testChangePassword_NotLoggedIn: Should throw AuthenticationException");
            } catch (AuthenticationException e) {
                System.out.println("[PASS] testChangePassword_NotLoggedIn_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testChangePassword_NotLoggedIn: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    /**
     * Test: Login with banned user (should succeed but with limited access)
     * Expected: Login successful, but user status is BANNED
     */
    public void testLogin_BannedUser_Success() {
        setUp();
        try {
            Set<UserRole> roles = EnumSet.of(UserRole.BUYER);
            User user = userService.register("testuser", "password123", roles);
            
            // Ban the user
            user.setStatus(enums.UserStatus.BANNED);
            
            // Banned user should be able to login
            User loggedIn = userService.login("testuser", "password123");
            
            assert loggedIn != null : "Banned user should be able to login";
            assert loggedIn.getStatus() == enums.UserStatus.BANNED : "Status should be BANNED";
            assert userService.isLoggedIn() : "Should be logged in";
            
            System.out.println("[PASS] testLogin_BannedUser_Success");
        } catch (Exception e) {
            System.out.println("[FAIL] testLogin_BannedUser: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    /**
     * Test: Change password with new password too short
     * Expected: BusinessException thrown
     */
    public void testChangePassword_NewPasswordTooShort_ThrowsException() {
        setUp();
        try {
            Set<UserRole> roles = EnumSet.of(UserRole.BUYER);
            userService.register("testuser", "oldpassword", roles);
            userService.login("testuser", "oldpassword");
            
            try {
                userService.changePassword("oldpassword", "12345");
                System.out.println("[FAIL] testChangePassword_NewPasswordTooShort: Should throw BusinessException");
            } catch (BusinessException e) {
                System.out.println("[PASS] testChangePassword_NewPasswordTooShort_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testChangePassword_NewPasswordTooShort: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    /**
     * Test: Get current user when not logged in
     * Expected: AuthenticationException thrown
     */
    public void testGetCurrentUser_NotLoggedIn_ThrowsException() {
        setUp();
        try {
            try {
                userService.getCurrentUser();
                System.out.println("[FAIL] testGetCurrentUser_NotLoggedIn: Should throw AuthenticationException");
            } catch (AuthenticationException e) {
                System.out.println("[PASS] testGetCurrentUser_NotLoggedIn_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testGetCurrentUser_NotLoggedIn: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    // ========== Query Methods Tests ==========
    
    /**
     * Test: Get user by ID successfully
     * Expected: User retrieved
     */
    public void testGetUserById_Success() {
        setUp();
        try {
            Set<UserRole> roles = EnumSet.of(UserRole.BUYER);
            User user = userService.register("testuser", "password123", roles);
            
            User retrieved = userService.getUserById(user.getUserId());
            
            assert retrieved != null : "User should not be null";
            assert user.getUserId().equals(retrieved.getUserId()) : "User IDs should match";
            assert "testuser".equals(retrieved.getUsername()) : "Username should match";
            
            System.out.println("[PASS] testGetUserById_Success");
        } catch (Exception e) {
            System.out.println("[FAIL] testGetUserById_Success: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    /**
     * Test: Get user by non-existent ID
     * Expected: ResourceNotFoundException thrown
     */
    public void testGetUserById_NotExists_ThrowsException() {
        setUp();
        try {
            try {
                userService.getUserById("U999");
                System.out.println("[FAIL] testGetUserById_NotExists: Should throw ResourceNotFoundException");
            } catch (exception.ResourceNotFoundException e) {
                System.out.println("[PASS] testGetUserById_NotExists_ThrowsException");
            }
        } catch (Exception e) {
            System.out.println("[FAIL] testGetUserById_NotExists: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    /**
     * Test: Get all sellers
     * Expected: Returns only users with SELLER role
     */
    public void testGetAllSellers_Success() {
        setUp();
        try {
            Set<UserRole> buyerRole = EnumSet.of(UserRole.BUYER);
            Set<UserRole> sellerRole = EnumSet.of(UserRole.SELLER);
            Set<UserRole> bothRoles = EnumSet.of(UserRole.BUYER, UserRole.SELLER);
            
            userService.register("buyer1", "password123", buyerRole);
            userService.register("seller1", "password123", sellerRole);
            userService.register("seller2", "password123", sellerRole);
            userService.register("both1", "password123", bothRoles);
            
            java.util.List<User> sellers = userService.getAllSellers();
            
            assert sellers.size() == 3 : "Should have 3 sellers (2 pure + 1 dual role)";
            
            System.out.println("[PASS] testGetAllSellers_Success");
        } catch (Exception e) {
            System.out.println("[FAIL] testGetAllSellers: " + e.getMessage());
        } finally {
            tearDown();
        }
    }
    
    // ========== Main Method to Run All Tests ==========
    
    public static void main(String[] args) {
        UserServiceTest test = new UserServiceTest();
        
        System.out.println("========================================");
        System.out.println("Running UserService Unit Tests");
        System.out.println("========================================\n");
        
        // Registration Tests
        test.testRegister_ValidInput_Success();
        test.testRegister_DuplicateUsername_ThrowsException();
        test.testRegister_UsernameTooShort_ThrowsException();
        test.testRegister_UsernameTooLong_ThrowsException();
        test.testRegister_UsernameInvalidChars_ThrowsException();
        test.testRegister_PasswordTooShort_ThrowsException();
        test.testRegister_PasswordTooLong_ThrowsException();
        test.testRegister_EmptyRoles_ThrowsException();
        test.testRegister_MultipleRoles_Success();
        
        // Login Tests
        test.testLogin_ValidCredentials_Success();
        test.testLogin_UsernameNotExists_ThrowsException();
        test.testLogin_WrongPassword_ThrowsException();
        test.testLogin_BannedUser_Success();
        test.testLogout_Success();
        
        // Password Change Tests
        test.testChangePassword_Success();
        test.testChangePassword_WrongOldPassword_ThrowsException();
        test.testChangePassword_NewPasswordTooShort_ThrowsException();
        test.testChangePassword_NotLoggedIn_ThrowsException();
        
        // Query Tests
        test.testGetCurrentUser_NotLoggedIn_ThrowsException();
        test.testGetUserById_Success();
        test.testGetUserById_NotExists_ThrowsException();
        test.testGetAllSellers_Success();
        
        System.out.println("\n========================================");
        System.out.println("All UserService Tests Completed");
        System.out.println("========================================");
    }
}

