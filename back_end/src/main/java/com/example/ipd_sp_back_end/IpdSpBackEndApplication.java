package com.example.ipd_sp_back_end;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@MapperScan("com.example.ipd_sp_back_end.mapper")
public class IpdSpBackEndApplication {

    public static void main(String[] args) {
        SpringApplication.run(IpdSpBackEndApplication.class, args);
    }

    // ============ 添加 CORS 配置 ============
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173", "http://localhost:4173", "http://127.0.0.1:4173")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("Content-Type", "Authorization", "X-Requested-With", "Accept", "Origin", "X-Account-Id")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
    // ============ 结束添加 ============
}
