package server.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import server.demo.interceptor.JwtInterceptor;
import server.demo.interceptor.StoreContextInterceptor;

/**
 * Web MVC配置
 * 配置拦截器链路。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Autowired
    private StoreContextInterceptor storeContextInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns(
                        "/api/v1/auth/login/password",
                        "/api/v1/auth/login/code",
                        "/api/v1/auth/register",
                        "/api/v1/auth/send-code",
                        "/api/v1/auth/reset-password",
                        "/api/v1/pricelabs/webhook/**",
                        "/api/v1/su/webhook/**",
                        "/api/v1/health"
                );

        registry.addInterceptor(storeContextInterceptor)
                .addPathPatterns("/api/v1/**");
    }
}
