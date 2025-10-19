import entity.Product;
import entity.User;
import enums.ProductCategory;
import enums.ProductCondition;
import enums.UserRole;
import factory.UserFactory;
import repository.DataCenter;
import service.ProductService;
import util.IdGenerator;

import java.util.EnumSet;

/**
 * 初始数据设置工具
 * 用于首次运行时创建示例数据
 */
public class InitialDataSetup {
    
    public static void setupInitialData() {
        DataCenter dc = DataCenter.getInstance();
        
        // 检查是否已有数据
        if (dc.getUserCount() > 0) {
            return; // 已有数据，不重复创建
        }
        
        System.out.println("[系统] 首次运行，创建示例数据...");
        
        // 创建管理员账户
        User admin = UserFactory.createAdmin("admin", "admin123");
        dc.addUser(admin);
        
        // 创建测试用户
        User buyer1 = UserFactory.createBuyer("zhangsan", "123456");
        dc.addUser(buyer1);
        
        User seller1 = UserFactory.createSeller("lisi", "123456");
        dc.addUser(seller1);
        
        User bothRole = UserFactory.createBuyerAndSeller("wangwu", "123456");
        dc.addUser(bothRole);
        
        // 创建测试商品
        ProductService ps = new ProductService();
        
        Product p1 = ps.publishProduct(seller1, 
            "iPhone 13 Pro", 
            "9成新，无划痕，配件齐全",
            4599.0,
            ProductCategory.ELECTRONICS,
            ProductCondition.LIKE_NEW);
        
        Product p2 = ps.publishProduct(seller1,
            "Java编程思想（第4版）",
            "经典教材，几乎全新",
            89.0,
            ProductCategory.BOOKS,
            ProductCondition.LIKE_NEW);
        
        Product p3 = ps.publishProduct(bothRole,
            "Nike运动鞋",
            "42码，穿过3次",
            299.0,
            ProductCategory.CLOTHING,
            ProductCondition.GOOD);
        
        Product p4 = ps.publishProduct(bothRole,
            "羽毛球拍（双拍）",
            "李宁N90，使用半年",
            350.0,
            ProductCategory.SPORTS,
            ProductCondition.GOOD);
        
        System.out.println("[系统] 示例数据创建完成！");
        System.out.println("[系统] 测试账户：");
        System.out.println("  管理员 - 用户名: admin, 密码: admin123");
        System.out.println("  买家   - 用户名: zhangsan, 密码: 123456");
        System.out.println("  卖家   - 用户名: lisi, 密码: 123456");
        System.out.println("  买卖家 - 用户名: wangwu, 密码: 123456");
        System.out.println();
    }
}


