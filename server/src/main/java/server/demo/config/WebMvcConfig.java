package server.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import server.demo.interceptor.JwtInterceptor;

/**
 * Web MVC配置
 * 配置JWT拦截器
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/v1/**") // 拦截所有API请求
                .excludePathPatterns(
                        "/api/v1/auth/login/password", // 排除密码登录
                        "/api/v1/auth/login/code", // 排除验证码登录
                        "/api/v1/auth/register", // 排除注册
                        "/api/v1/auth/send-code", // 排除发送验证码
                        "/api/v1/auth/reset-password", // 排除重置密码
                        "/api/v1/health" // 排除健康检查
                );
    }
}
