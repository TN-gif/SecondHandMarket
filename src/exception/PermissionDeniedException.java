package exception;

/**
 * 权限拒绝异常：无权限操作
 */
public class PermissionDeniedException extends BusinessException {
    
    public PermissionDeniedException(String message) {
        super(message);
    }
}


