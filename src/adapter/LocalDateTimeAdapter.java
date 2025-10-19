package adapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * LocalDateTime的自定义类型适配器
 * 
 * 问题：
 * Gson不支持JDK 8的时间API（LocalDateTime等），需要自定义适配器
 * 
 * 解决方案：
 * 将LocalDateTime序列化为"yyyy-MM-dd HH:mm:ss"格式的字符串
 * 反序列化时解析回LocalDateTime对象
 */
public class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {
    private static final DateTimeFormatter FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Override
    public void write(JsonWriter out, LocalDateTime value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(value.format(FORMATTER));
        }
    }
    
    @Override
    public LocalDateTime read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        String dateTimeStr = in.nextString();
        return LocalDateTime.parse(dateTimeStr, FORMATTER);
    }
}


