package net.suncaper.psychological.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 所有接口都允许跨域
        registry.addMapping("/**")
                // 前端地址（你的前端端口）
                .allowedOrigins("http://localhost:8081")
                // 允许所有请求方法
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // 允许带cookie
                .allowCredentials(true);
    }
}