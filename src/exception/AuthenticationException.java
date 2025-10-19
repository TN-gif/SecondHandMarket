package exception;

/**
 * 认证异常：登录失败、未登录等
 */
public class AuthenticationException extends BusinessException {
    
    public AuthenticationException(String message) {
        super(message);
    }
}


