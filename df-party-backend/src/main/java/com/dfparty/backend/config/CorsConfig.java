package com.dfparty.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${spring.profiles.active:local}")
    private String activeProfile;
    
    @Value("${CORS_ALLOWED_ORIGINS:https://www.dnfpartyhelper.xyz}")
    private String corsAllowedOrigins;
    
    @Value("${CORS_ALLOWED_METHODS:GET,POST,PUT,PATCH,DELETE,OPTIONS}")
    private String corsAllowedMethods;
    
    @Value("${CORS_ALLOWED_HEADERS:*}")
    private String corsAllowedHeaders;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if ("production".equals(activeProfile)) {
            // 운영 환경: 환경 변수에서 허용된 도메인만 허용
            String[] origins = corsAllowedOrigins.split(",");
            registry.addMapping("/**")
                    .allowedOrigins(origins)
                    .allowedMethods(corsAllowedMethods.split(","))
                    .allowedHeaders(corsAllowedHeaders.split(","))
                    .allowCredentials(false)
                    .maxAge(3600);
        } else {
            // 개발/로컬 환경: 모든 도메인 허용
            registry.addMapping("/**")
                    .allowedOriginPatterns("*")
                    .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .allowCredentials(false)
                    .maxAge(3600);
        }
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        if ("production".equals(activeProfile)) {
            // 운영 환경: 환경 변수에서 허용된 도메인만 허용
            String[] origins = corsAllowedOrigins.split(",");
            configuration.setAllowedOrigins(Arrays.asList(origins));
        } else {
            // 개발/로컬 환경: 모든 도메인 허용
            configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        }
        
        configuration.setAllowedMethods(Arrays.asList(corsAllowedMethods.split(",")));
        configuration.setAllowedHeaders(Arrays.asList(corsAllowedHeaders.split(",")));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
} 