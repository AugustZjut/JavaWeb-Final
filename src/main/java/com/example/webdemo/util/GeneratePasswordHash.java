package com.example.webdemo.util;

/**
 * 生成密码哈希的工具类
 */
public class GeneratePasswordHash {
    public static void main(String[] args) {
        try {
            String rawPassword = "admin123";
            String hashedPassword = CryptoUtils.generateSM3Hash(rawPassword);
            
            System.out.println("====================================");
            System.out.println("原始密码: " + rawPassword);
            System.out.println("SM3哈希: " + hashedPassword);
            System.out.println("哈希长度: " + hashedPassword.length() + "字符");
            System.out.println("====================================");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
