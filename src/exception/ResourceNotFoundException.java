package exception;

/**
 * 资源未找到异常：用户、商品、订单等不存在
 */
public class ResourceNotFoundException extends BusinessException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
}


