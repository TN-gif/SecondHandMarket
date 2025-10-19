import repository.DataCenter;
import service.*;
import util.DataPersistenceManager;

/**
 * 应用上下文 - 依赖注入容器
 * 
 * 设计思想：
 * 1. 集中管理所有Service的创建和依赖关系
 * 2. 体现控制反转（IoC）原理
 * 3. 使得Main类和Handler类只需要关注业务流程，不关心对象创建
 * 
 * 答辩要点：
 * 这个类实现了最朴素的依赖注入（Dependency Injection）。
 * 在企业级框架（如Spring）中，这个职责由IoC容器承担。
 * 通过手动实现这个机制，展示了对DI和IoC原理的深刻理解。
 * 
 * 核心优势：
 * 1. 依赖关系清晰：所有的依赖在构造器中一目了然
 * 2. 易于测试：可以轻松替换Service实现（如mock对象）
 * 3. 易于维护：新增Service只需在这里配置
 */
public class AppContext {
    
    // ========== Service实例（final确保只初始化一次） ==========
    
    public final DataCenter dataCenter;
    public final NotificationService notificationService;
    public final UserService userService;
    public final ProductService productService;
    public final OrderService orderService;
    public final ReviewService reviewService;
    public final AdminService adminService;
    
    // ========== Utility实例 ==========
    
    public final DataPersistenceManager persistenceManager;
    
    /**
     * 构造器：在这里统一管理所有依赖关系
     * 
     * 依赖关系图：
     * NotificationService ← UserService
     *                    ← OrderService
     * UserService ← AdminService
     * DataCenter ← 所有Service
     */
    public AppContext() {
        System.out.println("[系统] 正在初始化应用上下文...");
        
        // 1. 初始化没有依赖的组件
        this.dataCenter = DataCenter.getInstance();  // 单例
        this.notificationService = new NotificationService();
        
        // 2. 初始化有依赖的Service（依赖注入）
        this.userService = new UserService(notificationService);
        this.productService = new ProductService();
        this.orderService = new OrderService(notificationService);
        this.reviewService = new ReviewService();
        this.adminService = new AdminService(userService);
        
        // 3. 初始化工具类
        this.persistenceManager = new DataPersistenceManager();
        
        System.out.println("[系统] 应用上下文初始化完成");
    }
    
    /**
     * 启动时加载数据
     */
    public void loadData() {
        persistenceManager.loadAll();
    }
    
    /**
     * 关闭时保存数据
     */
    public void saveData() {
        persistenceManager.saveAll();
    }
}


