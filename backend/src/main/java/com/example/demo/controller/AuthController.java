package com.example.demo.controller;

import com.example.demo.mapper.SystemMapper;
import com.example.demo.utils.PasswordUtils;
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

        // 1. 加盐加密
        String encryptedPassword = PasswordUtils.encrypt(password, username);
        
        System.out.println("DEBUG: 用户 " + username + " 尝试登录");
        
        // 2. 查库
        Integer count = systemMapper.checkUserLogin(username, encryptedPassword);

        if (count != null && count > 0) {
            // 3. 签发 Token
            String token = Jwts.builder()
                    .setSubject(username)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                    .signWith(SignatureAlgorithm.HS256, SECRET_KEY) // 统一使用 String 签发
                    .compact();
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("msg", "登录成功");
            result.put("token", token);
            result.put("user", username);
            return ResponseEntity.ok(result);
        } else {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 401);
            result.put("msg", "用户名或密码错误，请检查！");
            return ResponseEntity.status(401).body(result);
        }
    }
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String realName = body.get("realName");

        // 1. 查重名
        Integer exists = systemMapper.checkUserExists(username);
        if (exists != null && exists > 0) {
            return ResponseEntity.status(400).body("该用户名已被占用");
        }

        // 2. 存入数据库
        try {
            // 👇👇👇 核心修改点：注册时也要进行加盐加密！
            String encryptedPassword = PasswordUtils.encrypt(password, username); 
            systemMapper.insertNewUser(username, encryptedPassword, realName != null ? realName : "新操作员");
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("数据库写入失败");
        }
    }
}