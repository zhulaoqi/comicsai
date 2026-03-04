package com.comicsai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.file-storage.base-path:./uploads}")
    private String fileStorageBasePath;

    @Value("${app.file-storage.access-url-prefix:/files}")
    private String accessUrlPrefix;

    /** Comma-separated allowed origins, e.g. http://localhost:5173,https://reader.example.com */
    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    private final JwtInterceptor jwtInterceptor;

    public WebMvcConfig(JwtInterceptor jwtInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = allowedOrigins.split(",");
        registry.addMapping("/api/**")
                .allowedOriginPatterns(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
        registry.addMapping(accessUrlPrefix + "/**")
                .allowedOriginPatterns(origins)
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(86400);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = java.nio.file.Paths.get(fileStorageBasePath)
                .toAbsolutePath().normalize().toString();
        registry.addResourceHandler(accessUrlPrefix + "/**")
                .addResourceLocations("file:" + absolutePath + "/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/reader/auth/register",
                        "/api/reader/auth/login",
                        "/api/reader/oauth/**",
                        "/api/admin/auth/login"
                );
    }
}
