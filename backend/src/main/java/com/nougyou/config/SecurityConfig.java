package com.nougyou.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nougyou.common.api.ApiResponse;
import com.nougyou.common.api.ResultCode;
import com.nougyou.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // SSE（SseEmitter）结束时 Servlet 容器会发起一次 ASYNC 分派，
                        // 此时 JWT 过滤器不会二次执行，SecurityContext 只剩匿名身份，
                        // 若再鉴权就会在响应已提交的情况下抛 403 并强行断开连接，
                        // 前端 reader.read() 将永远等不到流结束（停止生成按钮不消失）。
                        // 因此只对最初的 REQUEST 分派鉴权。
                        .shouldFilterAllDispatcherTypes(false)
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/register",
                                "/api/v1/auth/captcha",
                                "/api/v1/auth/refresh-token",
                                "/error",
                                // 农家乐项目公开数据：游客可匿名浏览列表与详情
                                "/api/v1/projects",
                                "/api/v1/projects/**",
                                "/api/projects",
                                "/api/projects/**",
                                // 农产品公开数据：游客可匿名浏览分类树、商品列表与详情
                                "/api/v1/categories",
                                "/api/v1/categories/**",
                                "/api/categories",
                                "/api/categories/**",
                                "/api/v1/products",
                                "/api/v1/products/**",
                                "/api/products",
                                "/api/products/**",
                                // 首页聚合数据：游客可匿名浏览政策、资讯、推荐项目与热销商品
                                "/api/v1/home",
                                "/api/home"
                        ).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint((request, response, ex) ->
                                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                                        ApiResponse.fail(ResultCode.ACCESS_TOKEN_INVALID, "访问令牌无效或已过期")))
                        .accessDeniedHandler((request, response, ex) ->
                                writeJson(response, HttpServletResponse.SC_FORBIDDEN,
                                        ApiResponse.fail(ResultCode.PERMISSION_DENIED, "权限不足"))))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void writeJson(HttpServletResponse response, int status, ApiResponse<?> body) throws java.io.IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
