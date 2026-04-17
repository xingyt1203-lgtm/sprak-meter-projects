package com.example.demo.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class PasswordUtils {
    public static String encrypt(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            String data = password + salt;
            byte[] bytes = md.digest(data.getBytes());
            return Base64.getEncoder().encodeToString(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 算法不可用", e);
        }
    }

    public static boolean check(String inputPassword, String salt, String dbPassword) {
        if (dbPassword == null) return false;
        return encrypt(inputPassword, salt).equals(dbPassword);
    }
}
