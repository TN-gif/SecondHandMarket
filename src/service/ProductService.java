package service;

import dto.SearchCriteria;
import entity.Product;
import entity.User;
import enums.ProductCategory;
import enums.ProductCondition;
import enums.ProductStatus;
import enums.UserRole;
import enums.UserStatus;
import exception.BusinessException;
import exception.PermissionDeniedException;
import exception.ResourceNotFoundException;
import repository.DataCenter;
import strategy.SortStrategy;
import util.IdGenerator;
import util.InputValidator;
import util.SimpleLogger;
import util.ValidationUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品服务
 * 
 * 核心功能：
 * 1. 商品发布、编辑、下架
 * 2. 商品搜索、筛选、排序
 * 3. 权限校验（只有卖家可以发布，只能操作自己的商品）
 * 
 * 答辩要点：
 * - 权限校验内聚：每个操作方法内部都进行权限检查
 * - RESERVED状态管理：支持严谨的状态流转
 * - Stream API：搜索和筛选使用流式处理
 */
public class ProductService {
    
    private static final SimpleLogger logger = SimpleLogger.getLogger(ProductService.class);
    
    private final DataCenter dataCenter;
    
    public ProductService() {
        this.dataCenter = DataCenter.getInstance();
    }
    
    // ========== 商品发布与管理 ==========
    
    /**
     * 发布商品
     * 
     * @param currentUser 当前用户（必须是卖家）
     */
    public Product publishProduct(User currentUser, String title, String description,
                                   double price, ProductCategory category, 
                                   ProductCondition condition) {
        logger.info("Publish product request: sellerId={}, title={}, price={}", 
                   currentUser.getUserId(), title, price);
        
        try {
            // 1. 用户状态检查
            if (currentUser.getStatus() == UserStatus.BANNED) {
                logger.warn("Publish failed: user banned - userId={}", currentUser.getUserId());
                throw new PermissionDeniedException("您的账号已被封禁，无法发布商品");
            }
            if (currentUser.getStatus() == UserStatus.DELETED) {
                logger.warn("Publish failed: user deleted - userId={}", currentUser.getUserId());
                throw new PermissionDeniedException("账号已被删除");
            }
            
            // 2. 权限校验
            if (!currentUser.hasRole(UserRole.SELLER)) {
                logger.warn("Publish failed: not a seller - userId={}", currentUser.getUserId());
                throw new PermissionDeniedException("只有卖家才能发布商品");
            }
            
            // 3. 输入验证
            ValidationUtils.validateProductTitle(title);
            ValidationUtils.validateProductDescription(description);
            ValidationUtils.validatePrice(price);
            
            // 4. 创建商品
            String productId = IdGenerator.generateProductId();
            Product product = new Product(productId, title, description, 
                                         price, category, condition, currentUser.getUserId());
            
            // 5. 保存到数据中心
            dataCenter.addProduct(product);
            
            logger.info("Product published successfully: productId={}, sellerId={}, title={}", 
                       productId, currentUser.getUserId(), title);
            
            return product;
            
        } catch (Exception e) {
            logger.error("Publish product failed: sellerId={}, error={}", 
                        currentUser.getUserId(), e.getMessage());
            throw e;
        }
    }
    
    /**
     * 编辑商品
     */
    public void editProduct(User currentUser, String productId, 
                           String newTitle, String newDescription, double newPrice) {
        logger.info("Edit product request: productId={}, sellerId={}", 
                   productId, currentUser.getUserId());
        
        try {
            // 1. 查找商品
            Product product = getProductById(productId);
            
            // 2. 权限校验：只能编辑自己的商品
            checkOwnership(currentUser, product);
            
            // 3. 状态检查：只有可售状态可以编辑
            if (product.getStatus() != ProductStatus.AVAILABLE) {
                logger.warn("Edit failed: product not available - productId={}, status={}", 
                           productId, product.getStatus());
                throw new BusinessException("当前状态的商品不能编辑");
            }
            
            // 4. 输入验证
            ValidationUtils.validateProductTitle(newTitle);
            ValidationUtils.validateProductDescription(newDescription);
            ValidationUtils.validatePrice(newPrice);
            
            // 5. 更新商品信息
            product.setTitle(newTitle);
            product.setDescription(newDescription);
            product.setPrice(newPrice);
            
            logger.info("Product edited successfully: productId={}", productId);
            
        } catch (Exception e) {
            logger.error("Edit product failed: productId={}, error={}", 
                        productId, e.getMessage());
            throw e;
        }
    }
    
    /**
     * 下架商品
     */
    public void removeProduct(User currentUser, String productId) {
        // 1. 查找商品
        Product product = getProductById(productId);
        
        // 2. 权限校验
        checkOwnership(currentUser, product);
        
        // 3. 下架
        product.remove();
    }
    
    /**
     * 重新上架商品
     */
    public void reListProduct(User currentUser, String productId) {
        // 1. 查找商品
        Product product = getProductById(productId);
        
        // 2. 权限校验
        checkOwnership(currentUser, product);
        
        // 3. 上架
        product.reList();
    }
    
    // ========== 商品查询 ==========
    
    /**
     * 根据ID获取商品
     */
    public Product getProductById(String productId) {
        return dataCenter.findProductById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在"));
    }
    
    /**
     * 获取卖家的所有商品
     */
    public List<Product> getProductsBySeller(String sellerId) {
        return dataCenter.findProductsBySeller(sellerId);
    }
    
    /**
     * 搜索商品（使用建造者模式的SearchCriteria）
     */
    public List<Product> searchProducts(SearchCriteria criteria) {
        return dataCenter.getAllProducts().stream()
                // 1. 状态过滤：只显示可售的商品（除非指定了其他状态）
                .filter(p -> criteria.getStatus() != null 
                        ? p.getStatus() == criteria.getStatus()
                        : p.getStatus() == ProductStatus.AVAILABLE)
                // 2. 关键词过滤
                .filter(p -> criteria.getKeyword() == null 
                        || p.getTitle().contains(criteria.getKeyword())
                        || p.getDescription().contains(criteria.getKeyword()))
                // 3. 分类过滤
                .filter(p -> criteria.getCategory() == null 
                        || p.getCategory() == criteria.getCategory())
                // 4. 成色过滤
                .filter(p -> criteria.getCondition() == null 
                        || p.getCondition() == criteria.getCondition())
                // 5. 价格范围过滤
                .filter(p -> criteria.getMinPrice() == null 
                        || p.getPrice() >= criteria.getMinPrice())
                .filter(p -> criteria.getMaxPrice() == null 
                        || p.getPrice() <= criteria.getMaxPrice())
                // 6. 卖家过滤
                .filter(p -> criteria.getSellerId() == null 
                        || p.getSellerId().equals(criteria.getSellerId()))
                .collect(Collectors.toList());
    }
    
    /**
     * 对商品列表进行排序（策略模式）
     */
    public void sortProducts(List<Product> products, SortStrategy<Product> strategy) {
        strategy.sort(products);
    }
    
    // ========== 权限校验 ==========
    
    /**
     * 检查用户是否是商品的所有者
     */
    private void checkOwnership(User user, Product product) {
        if (!product.getSellerId().equals(user.getUserId())) {
            throw new PermissionDeniedException("无权操作此商品");
        }
    }
    
    /**
     * 检查商品是否可以被购买
     */
    public void checkAvailableForPurchase(String productId, String buyerId) {
        Product product = getProductById(productId);
        
        // 1. 状态检查
        if (!product.isAvailable()) {
            throw new BusinessException("商品不可购买");
        }
        
        // 2. 不能购买自己的商品
        if (product.getSellerId().equals(buyerId)) {
            throw new BusinessException("不能购买自己的商品");
        }
    }
}


