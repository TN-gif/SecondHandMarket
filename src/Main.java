import entity.User;
import enums.UserRole;
import handler.AdminMenuHandler;
import handler.MainMenuHandler;
import handler.UserMenuHandler;
import util.ConsoleUtil;

/**
 * 主程序入口
 * 
 * 职责：
 * 1. 初始化应用上下文（AppContext）
 * 2. 主循环：根据登录状态选择不同的Handler
 * 3. 程序退出时保存数据
 * 
 * 设计说明：
 * Main类不再承担任何业务逻辑和视图渲染，只负责程序的启动、
 * 循环和关闭。所有的视图逻辑都委托给Handler类处理。
 * 
 * 这个Main类只有不到50行核心代码，职责单一，体现了良好的设计！
 */
public class Main {
    
    // 应用上下文（依赖注入容器）
    private static AppContext context;
    
    // Handler实例
    private static MainMenuHandler mainMenuHandler;
    private static UserMenuHandler userMenuHandler;
    private static AdminMenuHandler adminMenuHandler;
    
    public static void main(String[] args) {
        // 1. 初始化应用
        initialize();
        
        // 2. 显示欢迎信息
        ConsoleUtil.printTitle("校园二手商品交易管理系统 v1.0");
        System.out.println("欢迎使用！系统已启动。");
        System.out.println();
        
        // 3. 主循环
        mainLoop();
    }
    
    /**
     * 初始化应用
     */
    private static void initialize() {
        // 创建应用上下文（所有Service在这里被创建和注入）
        context = new AppContext();
        
        // 加载数据
        context.loadData();
        
        // 首次运行时创建示例数据
        InitialDataSetup.setupInitialData();
        
        // 创建Handler（从context获取Service实例）
        mainMenuHandler = new MainMenuHandler(context.userService, context.productService);
        userMenuHandler = new UserMenuHandler(context.userService, context.productService,
                                             context.orderService, context.reviewService);
        adminMenuHandler = new AdminMenuHandler(context.userService, context.adminService);
        
        // 注册关闭钩子（程序退出时自动保存数据）
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ConsoleUtil.printInfo("正在保存数据...");
            context.saveData();
        }));
    }
    
    /**
     * 主循环
     */
    private static void mainLoop() {
        while (true) {
            try {
                // 根据登录状态选择Handler
                if (!context.userService.isLoggedIn()) {
                    // 未登录：显示主菜单
                    mainMenuHandler.displayAndHandle();
                } else {
                    User currentUser = context.userService.getCurrentUser();
                    if (currentUser.hasRole(UserRole.ADMIN)) {
                        // 管理员：显示管理员菜单
                        adminMenuHandler.displayAndHandle();
                    } else {
                        // 普通用户：显示用户菜单
                        userMenuHandler.displayAndHandle();
                    }
                }
            } catch (exception.AuthenticationException e) {
                // 未登录异常，继续显示主菜单（静默处理）
            } catch (Exception e) {
                // 其他异常，显示错误信息
                ConsoleUtil.printError("操作失败：" + e.getMessage());
            }
        }
    }
}
