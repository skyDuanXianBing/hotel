package server.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson 配置类
 * 处理 Hibernate 懒加载代理对象的序列化问题
 */
@Configuration
public class JacksonConfig {

    /**
     * 配置 Jackson ObjectMapper
     * 注册 Hibernate6Module 来处理懒加载代理对象
     */
    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();

        // 注册 Hibernate 模块
        Hibernate6Module hibernate6Module = new Hibernate6Module();

        // 配置选项:
        // 1. 强制懒加载初始化 (false = 不强制,未加载的属性返回 null)
        hibernate6Module.configure(Hibernate6Module.Feature.FORCE_LAZY_LOADING, false);

        // 2. 序列化未初始化的代理为 null
        hibernate6Module.configure(Hibernate6Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS, false);

        objectMapper.registerModule(hibernate6Module);

        return objectMapper;
    }
}
