package entity;

import java.time.LocalDateTime;

/**
 * 消息实体类
 * 用于消息持久化
 */
public class Message {
    
    private String messageId;           // 消息ID
    private String userId;              // 接收用户ID
    private String content;             // 消息内容
    private LocalDateTime createTime;   // 创建时间
    private boolean read;               // 是否已读
    
    /**
     * 构造器
     */
    public Message(String messageId, String userId, String content) {
        this.messageId = messageId;
        this.userId = userId;
        this.content = content;
        this.createTime = LocalDateTime.now();
        this.read = false;
    }
    
    /**
     * 无参构造器（用于JSON反序列化）
     */
    public Message() {
    }
    
    // ========== Getters and Setters ==========
    
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    public boolean isRead() {
        return read;
    }
    
    public void setRead(boolean read) {
        this.read = read;
    }
    
    @Override
    public String toString() {
        return String.format("Message[id=%s, userId=%s, content=%s, read=%s]",
                messageId, userId, content, read);
    }
}

