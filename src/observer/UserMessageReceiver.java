package observer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户消息接收器 - 观察者实现
 * 
 * 设计说明：
 * 每个登录的用户都有一个UserMessageReceiver实例，
 * 用于接收系统通知、订单消息等。
 */
public class UserMessageReceiver implements MessageObserver {
    
    private final String userId;
    private final List<String> messages;
    private static final DateTimeFormatter TIME_FORMATTER = 
        DateTimeFormatter.ofPattern("MM-dd HH:mm");
    
    public UserMessageReceiver(String userId) {
        this.userId = userId;
        this.messages = new ArrayList<>();
    }
    
    @Override
    public void onMessage(String message) {
        // 格式化消息：添加时间戳
        String formattedMessage = String.format("[%s] %s", 
            LocalDateTime.now().format(TIME_FORMATTER), 
            message);
        messages.add(formattedMessage);
        
        // 实时打印通知（可选）
        System.out.println("[!] 新消息：" + message);
    }
    
    /**
     * 获取所有消息
     */
    public List<String> getMessages() {
        return new ArrayList<>(messages);
    }
    
    /**
     * 获取未读消息数量
     */
    public int getUnreadCount() {
        return messages.size();
    }
    
    /**
     * 清空消息
     */
    public void clearMessages() {
        messages.clear();
    }
    
    public String getUserId() {
        return userId;
    }
}


