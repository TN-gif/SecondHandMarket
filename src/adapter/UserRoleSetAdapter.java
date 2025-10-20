package adapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import enums.UserRole;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

/**
 * UserRole EnumSet的自定义类型适配器
 * 
 * 问题：
 * Gson默认情况下会将EnumSet序列化为复杂的对象结构，
 * 反序列化时可能出错或生成错误的集合类型。
 * 
 * 解决方案：
 * 将EnumSet<UserRole>序列化为简单的字符串数组：["BUYER", "SELLER"]
 * 反序列化时重新构建为EnumSet<UserRole>
 * 
 * 技术要点：
 * 1. 继承TypeAdapter<Set<UserRole>>
 * 2. 重写write和read方法
 * 3. 在GsonBuilder中注册此适配器
 * 
 * 答辩要点：
 * 这展示了对JSON序列化机制的深入理解。EnumSet是JDK内部优化的集合，
 * 其内部结构复杂（使用位向量），直接序列化会产生不必要的复杂度。
 * 自定义适配器将其转换为简单数组，既保证了数据的正确性，又提高了可读性。
 */
public class UserRoleSetAdapter extends TypeAdapter<Set<UserRole>> {
    
    @Override
    public void write(JsonWriter out, Set<UserRole> roles) throws IOException {
        if (roles == null || roles.isEmpty()) {
            out.beginArray();
            out.endArray();
            return;
        }
        
        // 将EnumSet写为JSON数组
        out.beginArray();
        for (UserRole role : roles) {
            out.value(role.name());  // 只写枚举名称
        }
        out.endArray();
    }
    
    @Override
    public Set<UserRole> read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return EnumSet.noneOf(UserRole.class);
        }
        
        // 从JSON数组读取并构建EnumSet
        Set<UserRole> roles = EnumSet.noneOf(UserRole.class);
        in.beginArray();
        while (in.hasNext()) {
            String roleName = in.nextString();
            try {
                roles.add(UserRole.valueOf(roleName));
            } catch (IllegalArgumentException e) {
                // 忽略无效的角色名称
                System.err.println("警告：忽略无效角色 " + roleName);
            }
        }
        in.endArray();
        
        return roles;
    }
}


