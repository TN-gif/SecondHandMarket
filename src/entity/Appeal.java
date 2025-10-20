package entity;

import java.time.LocalDateTime;

/**
 * 申诉实体类
 * 用于被封禁用户的解封申诉
 */
public class Appeal {
    
    private String appealId;            // 申诉ID
    private String userId;              // 申诉用户ID
    private String reason;              // 申诉理由
    private LocalDateTime createTime;   // 创建时间
    private boolean processed;          // 是否已处理
    private String result;              // 处理结果
    private LocalDateTime processTime;  // 处理时间
    
    /**
     * 构造器
     */
    public Appeal(String appealId, String userId, String reason) {
        this.appealId = appealId;
        this.userId = userId;
        this.reason = reason;
        this.createTime = LocalDateTime.now();
        this.processed = false;
    }
    
    /**
     * 无参构造器（用于JSON反序列化）
     */
    public Appeal() {
    }
    
    /**
     * 处理申诉
     */
    public void process(String result) {
        this.processed = true;
        this.result = result;
        this.processTime = LocalDateTime.now();
    }
    
    // ========== Getters and Setters ==========
    
    public String getAppealId() {
        return appealId;
    }
    
    public void setAppealId(String appealId) {
        this.appealId = appealId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    public boolean isProcessed() {
        return processed;
    }
    
    public void setProcessed(boolean processed) {
        this.processed = processed;
    }
    
    public String getResult() {
        return result;
    }
    
    public void setResult(String result) {
        this.result = result;
    }
    
    public LocalDateTime getProcessTime() {
        return processTime;
    }
    
    public void setProcessTime(LocalDateTime processTime) {
        this.processTime = processTime;
    }
    
    @Override
    public String toString() {
        return String.format("Appeal[id=%s, userId=%s, processed=%s]",
                appealId, userId, processed);
    }
}

