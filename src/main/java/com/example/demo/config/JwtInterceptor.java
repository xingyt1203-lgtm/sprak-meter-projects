package com.example.demo.config;

import com.example.demo.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class JwtInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 放行 OPTIONS 请求（跨域预检）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 2. 检查是否是登录接口（防御性放行，虽然 WebConfig 已经配置了 exclude）
        String uri = request.getRequestURI();
        if (uri.contains("/auth/login")) {
            return true;
        }

        // 3. 校验 Token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\": 401, \"msg\": \"未携带鉴权 Token\"}");
            return false;
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = JwtUtils.parseToken(token);
            if (JwtUtils.isTokenExpired(claims)) {
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\": 401, \"msg\": \"Token 已过期\"}");
                return false;
            }
            request.setAttribute("userClaims", claims);
            return true;
        } catch (Exception e) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\": 401, \"msg\": \"Token 验证失败\"}");
            return false;
        }
    }
}