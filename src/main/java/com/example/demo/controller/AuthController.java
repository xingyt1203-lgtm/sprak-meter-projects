package com.example.demo.controller;

import com.example.demo.mapper.SystemMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final String SECRET_KEY = "spark-meter-demo-key";
    private static final long EXPIRATION = 1000 * 60 * 60 * 24; // 24小时

    @Autowired
    private SystemMapper systemMapper;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        // 真实查库！
        Integer count = systemMapper.checkUserLogin(username, password);

        if (count != null && count > 0) {
            String token = Jwts.builder()
                    .setSubject(username)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                    .signWith(SignatureAlgorithm.HS256, SECRET_KEY.getBytes())
                    .compact();
            
            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("user", username);
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(401).body("用户名或密码错误，请检查！");
        }
    }
}