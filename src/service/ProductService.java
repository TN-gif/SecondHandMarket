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
 * 负责商品的发布、编辑、状态管理和检索功能。
 * 权限检查内聚在每个操作方法内部，确保只有商品所有者可以修改自己的商品。
 * 使用Stream API实现高效的搜索和筛选功能。
 */
public class ProductService {
    
    private static final SimpleLogger logger = SimpleLogger.getLogger(ProductService.class);
    private final DataCenter dataCenter;
    
    public ProductService() {
        this.dataCenter = DataCenter.getInstance();
    }
    
    /**
     * 发布新商品
     * 
     * 只有卖家可以发布商品，被封禁或删除的账号无法发布。
     * 
     * @param currentUser 当前用户，必须拥有卖家角色
     * @param title 商品标题，2-100字符
     * @param description 商品描述，最多1000字符
     * @param price 商品价格，0.01-1000000，最多2位小数
     * @param category 商品分类
     * @param condition 商品成色
     * @return 创建的商品对象
     * @throws PermissionDeniedException 如果用户没有卖家角色或账号状态异常
     * @throws BusinessException 如果输入验证失败
     */
    public Product publishProduct(User currentUser, String title, String description,
                                   double price, ProductCategory category, 
                                   ProductCondition condition) {
        logger.info("Publish product request: sellerId={}, title={}, price={}", 
                   currentUser.getUserId(), title, price);
        
        try {
            validateSellerStatus(currentUser);
            validateProductInput(title, description, price);
            
            String productId = IdGenerator.generateProductId();
            Product product = new Product(productId, title, description, 
                                         price, category, condition, currentUser.getUserId());
            
            dataCenter.addProduct(product);
            
            logger.info("Product published successfully: productId={}", productId);
            return product;
            
        } catch (Exception e) {
            logger.error("Publish product failed: sellerId={}, error={}", 
                        currentUser.getUserId(), e.getMessage());
            throw e;
        }
    }
    
    /**
     * 编辑商品信息
     * 
     * 只有商品所有者可以编辑，且只能编辑AVAILABLE状态的商品。
     * 
     * @param currentUser 当前用户
     * @param productId 商品ID
     * @param newTitle 新标题
     * @param newDescription 新描述
     * @param newPrice 新价格
     * @throws PermissionDeniedException 如果不是商品所有者
     * @throws BusinessException 如果商品状态不允许编辑或输入验证失败
     */
    public void editProduct(User currentUser, String productId, 
                           String newTitle, String newDescription, double newPrice) {
        logger.info("Edit product request: productId={}, sellerId={}", 
                   productId, currentUser.getUserId());
        
        try {
            Product product = getProductById(productId);
            checkOwnership(currentUser, product);
            
            if (product.getStatus() != ProductStatus.AVAILABLE) {
                throw new BusinessException("当前状态的商品不能编辑");
            }
            
            validateProductInput(newTitle, newDescription, newPrice);
            
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
     * 
     * @param currentUser 当前用户
     * @param productId 商品ID
     * @throws PermissionDeniedException 如果不是商品所有者
     */
    public void removeProduct(User currentUser, String productId) {
        Product product = getProductById(productId);
        checkOwnership(currentUser, product);
        product.remove();
    }
    
    /**
     * 重新上架商品
     * 
     * @param currentUser 当前用户
     * @param productId 商品ID
     * @throws PermissionDeniedException 如果不是商品所有者
     */
    public void reListProduct(User currentUser, String productId) {
        Product product = getProductById(productId);
        checkOwnership(currentUser, product);
        product.reList();
    }
    
    /**
     * 根据ID获取商品
     * 
     * @param productId 商品ID
     * @return 商品对象
     * @throws ResourceNotFoundException 如果商品不存在
     */
    public Product getProductById(String productId) {
        return dataCenter.findProductById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在"));
    }
    
    /**
     * 获取指定卖家的所有商品
     * 
     * @param sellerId 卖家ID
     * @return 商品列表
     */
    public List<Product> getProductsBySeller(String sellerId) {
        return dataCenter.findProductsBySeller(sellerId);
    }
    
    /**
     * 根据条件搜索商品
     * 
     * 使用建造者模式构建的SearchCriteria进行多维度筛选。
     * 默认只返回AVAILABLE状态的商品，除非在条件中明确指定其他状态。
     * 
     * @param criteria 搜索条件
     * @return 符合条件的商品列表
     */
    public List<Product> searchProducts(SearchCriteria criteria) {
        return dataCenter.getAllProducts().stream()
                .filter(p -> matchesStatus(p, criteria))
                .filter(p -> matchesKeyword(p, criteria))
                .filter(p -> matchesCategory(p, criteria))
                .filter(p -> matchesCondition(p, criteria))
                .filter(p -> matchesPriceRange(p, criteria))
                .filter(p -> matchesSeller(p, criteria))
                .collect(Collectors.toList());
    }
    
    /**
     * 对商品列表进行排序
     * 
     * 使用策略模式支持多种排序方式。
     * 
     * @param products 要排序的商品列表
     * @param strategy 排序策略
     */
    public void sortProducts(List<Product> products, SortStrategy<Product> strategy) {
        strategy.sort(products);
    }
    
    /**
     * 检查商品是否可以被购买
     * 
     * @param productId 商品ID
     * @param buyerId 买家ID
     * @throws BusinessException 如果商品不可购买或试图购买自己的商品
     */
    public void checkAvailableForPurchase(String productId, String buyerId) {
        Product product = getProductById(productId);
        
        if (!product.isAvailable()) {
            throw new BusinessException("商品不可购买");
        }
        
        if (product.getSellerId().equals(buyerId)) {
            throw new BusinessException("不能购买自己的商品");
        }
    }
    
    /**
     * 验证卖家状态
     * 
     * 被封禁或删除的账号无法发布商品。
     */
    private void validateSellerStatus(User user) {
        if (user.getStatus() == UserStatus.BANNED) {
            throw new PermissionDeniedException("您的账号已被封禁，无法发布商品");
        }
        if (user.getStatus() == UserStatus.DELETED) {
            throw new PermissionDeniedException("账号已被删除");
        }
        if (!user.hasRole(UserRole.SELLER)) {
            throw new PermissionDeniedException("只有卖家才能发布商品");
        }
    }
    
    /**
     * 验证商品输入
     */
    private void validateProductInput(String title, String description, double price) {
        ValidationUtils.validateProductTitle(title);
        ValidationUtils.validateProductDescription(description);
        ValidationUtils.validatePrice(price);
    }
    
    /**
     * 检查用户是否是商品所有者
     */
    private void checkOwnership(User user, Product product) {
        if (!product.getSellerId().equals(user.getUserId())) {
            throw new PermissionDeniedException("无权操作此商品");
        }
    }
    
    /**
     * 筛选器：匹配状态
     */
    private boolean matchesStatus(Product p, SearchCriteria criteria) {
        return criteria.getStatus() != null 
            ? p.getStatus() == criteria.getStatus()
            : p.getStatus() == ProductStatus.AVAILABLE;
    }
    
    /**
     * 筛选器：匹配关键词
     */
    private boolean matchesKeyword(Product p, SearchCriteria criteria) {
        return criteria.getKeyword() == null 
            || p.getTitle().contains(criteria.getKeyword())
            || p.getDescription().contains(criteria.getKeyword());
    }
    
    /**
     * 筛选器：匹配分类
     */
    private boolean matchesCategory(Product p, SearchCriteria criteria) {
        return criteria.getCategory() == null || p.getCategory() == criteria.getCategory();
    }
    
    /**
     * 筛选器：匹配成色
     */
    private boolean matchesCondition(Product p, SearchCriteria criteria) {
        return criteria.getCondition() == null || p.getCondition() == criteria.getCondition();
    }
    
    /**
     * 筛选器：匹配价格范围
     */
    private boolean matchesPriceRange(Product p, SearchCriteria criteria) {
        boolean minOk = criteria.getMinPrice() == null || p.getPrice() >= criteria.getMinPrice();
        boolean maxOk = criteria.getMaxPrice() == null || p.getPrice() <= criteria.getMaxPrice();
        return minOk && maxOk;
    }
    
    /**
     * 筛选器：匹配卖家
     */
    private boolean matchesSeller(Product p, SearchCriteria criteria) {
        return criteria.getSellerId() == null || p.getSellerId().equals(criteria.getSellerId());
    }
}


