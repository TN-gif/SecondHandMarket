# 校园二手商品交易管理系统

> 基于Java的命令行二手交易平台，完整实现用户管理、商品管理、订单管理和评价管理功能。

## 项目特色

### 🎯 核心亮点

1. **EnumSet角色管理** - 类型安全的多角色支持（买家+卖家）
2. **观察者模式消息通知** - 完善的生命周期管理（登录订阅，登出退订）
3. **AppContext依赖注入** - 显式化DI思想，体现IoC原理
4. **Handler层分离** - 避免"上帝类"，职责单一
5. **RESERVED中间状态** - 严谨的商品状态流转
6. **EnumSet自定义序列化** - 确保数据持久化稳定
7. **智能翻译机制** - 三层翻译策略（离线映射 + 词汇表 + 百度API）

### 📐 设计模式（5种）

- **单例模式（Singleton）** - DataCenter数据中心
- **工厂模式（Factory）** - UserFactory用户创建
- **策略模式（Strategy）** - ProductSortStrategies商品排序
- **建造者模式（Builder）** - SearchCriteria搜索条件
- **观察者模式（Observer）** - NotificationService消息通知

### 🏗️ 架构设计

```
Main + AppContext（依赖注入容器）
    ↓
Handler层（视图控制器）
    ↓
Service层（业务逻辑）
    ↓
Repository层（数据访问）
```

## 技术栈

- **JDK 17** - 使用现代Java特性（Lambda、Stream、Record、Switch表达式）
- **Gson 2.10.1** - JSON序列化/反序列化
- **自定义TypeAdapter** - EnumSet和LocalDateTime序列化
- **百度翻译API** - 智能中英文翻译（用于表格国际化显示）
- **FlipTables 1.1.0** - 专业表格格式化输出

## 项目结构

```
SecondHandMarket/
├── src/
│   ├── Main.java                    # 主程序入口（<50行）
│   ├── AppContext.java              # 依赖注入容器
│   ├── entity/                      # 实体类
│   │   ├── User.java               # 用户（EnumSet角色）
│   │   ├── Product.java            # 商品（RESERVED状态）
│   │   ├── Order.java              # 订单
│   │   └── Review.java             # 评价
│   ├── enums/                      # 枚举类
│   │   ├── UserRole.java          # 用户角色
│   │   ├── ProductStatus.java     # 商品状态
│   │   └── ...
│   ├── service/                    # 业务逻辑层
│   │   ├── UserService.java       # 用户服务
│   │   ├── ProductService.java    # 商品服务
│   │   ├── OrderService.java      # 订单服务
│   │   ├── ReviewService.java     # 评价服务
│   │   └── NotificationService.java # 通知服务
│   ├── handler/                    # 视图控制器
│   │   ├── MainMenuHandler.java   # 主菜单
│   │   ├── UserMenuHandler.java   # 用户菜单
│   │   └── AdminMenuHandler.java  # 管理员菜单
│   ├── repository/                 # 数据访问层
│   │   └── DataCenter.java        # 单例数据中心
│   ├── factory/                    # 工厂模式
│   │   └── UserFactory.java
│   ├── strategy/                   # 策略模式
│   │   ├── SortStrategy.java
│   │   └── ProductSortStrategies.java
│   ├── observer/                   # 观察者模式
│   │   ├── MessageObserver.java
│   │   └── UserMessageReceiver.java
│   ├── dto/                        # 数据传输对象
│   │   └── SearchCriteria.java    # 建造者模式
│   ├── adapter/                    # 类型适配器
│   │   ├── UserRoleSetAdapter.java
│   │   └── LocalDateTimeAdapter.java
│   ├── util/                       # 工具类
│   │   ├── IdGenerator.java
│   │   ├── PasswordEncoder.java
│   │   ├── InputValidator.java
│   │   ├── ConsoleUtil.java
│   │   ├── DataPersistenceManager.java
│   │   ├── BaiduTranslateAPI.java   # 百度翻译API
│   │   ├── TranslationUtil.java     # 翻译工具类
│   │   └── PerfectTableFormatter.java # 表格格式化
│   └── exception/                  # 自定义异常
│       ├── BusinessException.java
│       ├── AuthenticationException.java
│       ├── ResourceNotFoundException.java
│       └── PermissionDeniedException.java
├── lib/
│   ├── gson-2.10.1.jar            # Gson库
│   └── fliptables-1.1.0.jar       # 表格格式化库
├── data/                           # 数据文件（自动生成）
│   ├── users.json
│   ├── products.json
│   ├── orders.json
│   ├── reviews.json
│   └── messages.json
├── config.properties.example       # API配置示例（上交）
├── config.properties              # API配置文件（本地使用，不上交）
├── .gitignore                     # Git忽略文件配置
└── README.md
```

## 🔧 配置说明

### 百度翻译API配置（重要）

系统使用百度翻译API实现中英文翻译，用于表格显示的国际化。首次使用需要配置API密钥：

#### 步骤1：创建配置文件

```bash
# 将示例配置文件复制为正式配置文件
copy config.properties.example config.properties
```

#### 步骤2：获取API密钥

1. 访问 [百度翻译开放平台](https://fanyi-api.baidu.com/)
2. 注册/登录百度账号
3. 进入管理控制台
4. 创建应用，获取 `APP ID` 和 `密钥`

#### 步骤3：填写配置文件

编辑 `config.properties` 文件：

```properties
# 百度翻译API配置
baidu.translate.appid=YOUR_APP_ID_HERE
baidu.translate.key=YOUR_SECURITY_KEY_HERE
```

将 `YOUR_APP_ID_HERE` 和 `YOUR_SECURITY_KEY_HERE` 替换为您的真实API信息。

#### 说明

- **免费额度**：200万字符/月（对校园项目完全够用）
- **QPS限制**：1次/秒（系统已自动限流）
- **降级模式**：如果未配置，系统仍可运行，但表格显示可能为中文

#### ⚠️ 安全提示

- ❌ **请勿上传** `config.properties` 文件到公开仓库（已在 `.gitignore` 中排除）
- ✅ **请上交** `config.properties.example` 示例配置文件
- ✅ **请上交** 所有源代码和文档

---

## 编译和运行

### 前置条件

1. ✅ JDK 17 或更高版本
2. ✅ 配置百度翻译API（参考上方配置说明）

### 方法1：使用批处理脚本（推荐）

```bash
# 1. 编译
.\compile.bat

# 2. 运行
.\run.bat
```

### 方法2：使用命令行

```bash
# 1. 编译（包含Gson库）
javac -encoding UTF-8 -cp "lib/*" -d out/production/SecondHandMarket src/*.java src/*/*.java

# 2. 运行
java -cp "out/production/SecondHandMarket;lib/*" Main
```

### 方法3：使用IntelliJ IDEA

1. 打开项目
2. 右键 `lib/gson-2.10.1.jar` → Add as Library
3. 创建并配置 `config.properties`（参考上方配置说明）
4. 运行 `Main.java`

## 功能说明

### 用户功能

#### 买家
- ✅ 浏览商品（支持搜索、筛选、排序）
- ✅ 下单购买
- ✅ 查看我的订单
- ✅ 确认收货
- ✅ 评价订单

#### 卖家
- ✅ 发布商品
- ✅ 管理商品（编辑、下架、重新上架）
- ✅ 查看订单
- ✅ 确认订单

#### 管理员
- ✅ 用户管理（封禁、解封）
- ✅ 系统统计

### 核心流程

#### 1. 完整交易流程

```
买家下单 → 商品变为RESERVED
    ↓
卖家确认订单 → 订单状态：PENDING → CONFIRMED
    ↓
买家确认收货 → 订单状态：CONFIRMED → COMPLETED
    ↓          商品状态：RESERVED → SOLD
买家评价 → 更新卖家信誉分
```

#### 2. 订单取消流程

```
任意一方取消订单
    ↓
订单状态 → CANCELLED
    ↓
商品状态：RESERVED → AVAILABLE（恢复可售）
```

### 技术亮点详解

#### 1. EnumSet角色管理

```java
// 用户可以同时是买家和卖家
Set<UserRole> roles = EnumSet.of(UserRole.BUYER, UserRole.SELLER);
User user = new User(id, username, password, roles);

// 类型安全的角色检查
if (user.hasRole(UserRole.BUYER)) {
    // 买家操作
}
```

**优势**：
- 类型安全，编译期检查
- 高性能（位向量实现）
- 易扩展（新增角色无需改代码结构）

#### 2. 观察者模式生命周期管理

```java
// 登录时订阅
UserMessageReceiver receiver = new UserMessageReceiver(userId);
notificationService.subscribe(userId, receiver);

// 登出时退订
notificationService.unsubscribe(userId, receiver);
```

**优势**：
- 避免内存泄漏
- 消息精准送达
- 松耦合设计

#### 3. AppContext依赖注入

```java
public AppContext() {
    this.notificationService = new NotificationService();
    this.userService = new UserService(notificationService);
    this.orderService = new OrderService(notificationService);
}
```

**优势**：
- 依赖关系清晰
- 易于测试（可mock）
- 体现IoC原理

#### 4. RESERVED中间状态

```java
// 下单：AVAILABLE → RESERVED
product.reserve();

// 取消：RESERVED → AVAILABLE
product.cancelReservation();

// 完成：RESERVED → SOLD
product.markAsSold();
```

**优势**：
- 逻辑严谨
- 状态流转清晰
- 符合真实业务场景

#### 5. EnumSet序列化

```java
public class UserRoleSetAdapter extends TypeAdapter<Set<UserRole>> {
    @Override
    public void write(JsonWriter out, Set<UserRole> roles) {
        // 序列化为 ["BUYER", "SELLER"]
    }
    
    @Override
    public Set<UserRole> read(JsonReader in) {
        // 反序列化为 EnumSet
    }
}
```

**优势**：
- 数据格式简洁
- 序列化稳定
- 可读性强

## 数据持久化

系统使用JSON格式保存数据，文件位于 `data/` 目录：

- `users.json` - 用户数据
- `products.json` - 商品数据
- `orders.json` - 订单数据
- `reviews.json` - 评价数据

数据在程序退出时自动保存（通过关闭钩子）。

## 示例数据

### 用户

```json
{
  "userId": "U17294567891",
  "username": "zhangsan",
  "roles": ["BUYER", "SELLER"],
  "reputation": 100,
  "status": "ACTIVE"
}
```

### 商品

```json
{
  "productId": "P17294567892",
  "title": "iPhone 13",
  "price": 3999.0,
  "category": "ELECTRONICS",
  "condition": "LIKE_NEW",
  "status": "AVAILABLE",
  "sellerId": "U17294567891"
}
```

## 答辩要点

### Q1: 为什么使用EnumSet而不是多个boolean字段？

**A**: EnumSet有四大优势：
1. **类型安全** - 编译期检查，不会拼写错误
2. **高性能** - 内部使用位向量，空间效率高
3. **易扩展** - 新增角色只需添加枚举值
4. **表达力强** - `Set<UserRole>`比`isBuyer/isSeller`更清晰

### Q2: 为什么需要RESERVED状态？

**A**: 解决订单取消后商品状态恢复问题：
- 如果没有RESERVED，下单后商品变SOLD，取消时无法判断应恢复为什么状态
- 引入RESERVED后，状态流转清晰：`AVAILABLE → RESERVED → SOLD`
- 取消时直接从RESERVED恢复为AVAILABLE

### Q3: AppContext实现了什么设计思想？

**A**: 依赖注入（DI）和控制反转（IoC）：
- 集中管理所有Service的创建和依赖关系
- 对象创建控制权从使用者转移到容器
- 这是Spring等框架的核心思想
- 优势：依赖清晰、易测试、易扩展

### Q4: 观察者模式如何管理生命周期？

**A**: 
- **登录时订阅** - 创建MessageReceiver并注册到NotificationService
- **登出时退订** - 从NotificationService移除，避免内存泄漏
- **消息送达** - 只通知已登录用户
- **松耦合** - Service层无需知道Observer实现细节

### Q5: 为什么要拆分Handler层？

**A**: 避免"上帝类"：
- 如果把所有菜单逻辑写在Main中，Main会变得臃肿
- 通过Handler层按角色分离逻辑
- Main只负责程序入口和主循环（<50行）
- 符合单一职责原则

## 作者

- 课程：Java程序设计课程设计
- 版本：终极版（99分方案）
- JDK版本：17

## 开发计划

- [x] Day 1: 实体类和枚举类
- [x] Day 2: Service层
- [x] Day 3: AppContext和Handler层
- [x] Day 4: 设计模式应用
- [x] Day 5: 观察者模式
- [x] Day 6: 数据持久化
- [x] Day 7: 测试与完善

## 预期成果

- ✅ 完整的交易流程
- ✅ EnumSet角色管理
- ✅ RESERVED状态流转
- ✅ 观察者消息通知
- ✅ 数据持久化
- ✅ 5种设计模式
- ✅ 工程实践完善

**预期得分：96-99分**


