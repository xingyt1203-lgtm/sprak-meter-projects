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
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(401);
            response.getWriter().write("{\"code\": 401, \"msg\": \"未携带鉴权 Token\"}");
            return false;
        }
        String token = authHeader.substring(7);
        try {
            Claims claims = JwtUtils.parseToken(token);
            if (JwtUtils.isTokenExpired(claims)) {
                response.setStatus(401);
                response.getWriter().write("{\"code\": 401, \"msg\": \"Token 已过期\"}");
                return false;
            }
            request.setAttribute("userClaims", claims);
            return true;
        } catch (Exception e) {
            response.setStatus(401);
            response.getWriter().write("{\"code\": 401, \"msg\": \"Token 验证失败\"}");
            return false;
        }
    }
}
