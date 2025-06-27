package com.example.webdemo.util;

import java.io.InputStream;
import java.util.Properties;

/**
 * 一个临时的工具类，用于生成加密的模拟数据，以便填充到 mock_data.sql 文件中。
 * <p>
 * <b>使用方法:</b>
 * 1. 确保您的加密逻辑在 {@link CryptoUtils} 中已经实现并且可以正常工作。
 * 2. 确保 {@code pom.xml} 中包含了 Bouncy Castle 等必要的加密库依赖。
 * 3. 直接运行此类的 {@code main} 方法。
 * 4. 将控制台输出的加密后的 Base64 字符串复制到 {@code mock_data.sql} 文件中对应的位置。
 * </p>
 */
public class MockDataEncryptor {

    public static void main(String[] args) {
        try {
            // --- 1. 加载配置文件和密钥 ---
            Properties props = new Properties();
            // db.properties 文件应该在 classpath 的根目录下
            try (InputStream input = MockDataEncryptor.class.getClassLoader().getResourceAsStream("db.properties")) {
                if (input == null) {
                    System.out.println("Sorry, unable to find db.properties");
                    return;
                }
                props.load(input);
            }

            String sm4KeyHex = props.getProperty("sm4.keyHex"); // 获取SM4密钥

            System.out.println("Successfully loaded encryption keys.");
            System.out.println("--- Generating Encrypted Mock Data ---");

            // --- 2. 定义要加密的明文模拟数据 ---
            // 注意：这里的明文数据仅为示例，您可以替换为任何您想用的测试数据
            String[] phoneNumbers = {
                "13800138001", // for sysadmin
                "13800138002", // for schooladmin
                "13800138003", // for cs_admin
                "13800138004", // for security_audit
                "13800138005", // for ee_admin
                "13900139001", // for applicant 张三
                "13900139002", // for applicant 李四
                "13900139003", // for applicant 王五
                "13900139004", // for applicant 赵六
                "13900139005", // for accompanying person 赵六的妻子
                "13900139006"  // for accompanying person 赵六的儿子
            };

            String[] idCards = {
                "110101199003071111", // for applicant 张三
                "110101199003072222", // for applicant 李四
                "110101199003073333", // for applicant 王五
                "110101199003074444", // for applicant 赵六
                "110101199003075555", // for accompanying person 赵六的妻子
                "110101199003076666"  // for accompanying person 赵六的儿子
            };
            
            String[] names = {
                "张三", // for applicant 张三
                "李四", // for applicant 李四
                "王五", // for applicant 王五
                "赵六", // for applicant 赵六
                "陈美丽", // for accompanying person 赵六的妻子
                "赵小明"  // for accompanying person 赵六的儿子
            };

            // --- 3. 执行加密并打印结果 ---
            System.out.println("\n--- Encrypted Phones (for users, using SM4) ---");
            char placeholderChar = 'a';
            for (int i = 0; i < phoneNumbers.length; i++) {
                String encryptedPhone = CryptoUtils.encryptSM4(phoneNumbers[i], sm4KeyHex);
                System.out.println("Encrypted phone for placeholder_" + (placeholderChar++) + ": " + encryptedPhone);
            }

            System.out.println("\n--- Encrypted ID Cards (for appointments & accompanying_persons, using SM4) ---");
            // Start from placeholder 'a' for applicants and accompanying persons
            placeholderChar = 'a';
            for (int i = 0; i < idCards.length; i++) {
                String encryptedIdCard = CryptoUtils.encryptSM4(idCards[i], sm4KeyHex);
                System.out.println("Encrypted id_card for placeholder_" + (placeholderChar++) + ": " + encryptedIdCard);
            }
            
            System.out.println("\n--- Encrypted Names (for appointments & accompanying_persons, using SM4) ---");
            // Start from placeholder 'a' for applicants and accompanying persons
            placeholderChar = 'a';
            for (int i = 0; i < names.length; i++) {
                String encryptedName = CryptoUtils.encryptSM4(names[i], sm4KeyHex);
                System.out.println("Encrypted name for placeholder_" + (placeholderChar++) + ": " + encryptedName);
            }

            System.out.println("\n--- Generation Complete ---");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
