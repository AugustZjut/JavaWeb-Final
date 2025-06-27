package com.example.webdemo.util;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class CryptoUtils {

    static {
        // 添加BouncyCastle作为安全提供者
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    // --- SM2 (公钥加密) ---

    /**
     * 生成SM2密钥对。
     * @return KeyPair
     */
    public static KeyPair generateSM2KeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
        kpg.initialize(new ECGenParameterSpec("sm2p256v1"), new SecureRandom());
        return kpg.generateKeyPair();
    }

    /**
     * 从十六进制字符串加载SM2公钥。
     * @param hexPublicKey 十六进制公钥字符串
     * @return PublicKey
     */
    public static PublicKey getPublicKeyFromHex(String hexPublicKey) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        KeyFactory keyFactory = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
        byte[] decodedKey = Hex.decode(hexPublicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKey);
        return keyFactory.generatePublic(keySpec);
    }

    /**
     * 从十六进制字符串加载SM2私钥。
     * @param hexPrivateKey 十六进制私钥字符串
     * @return PrivateKey
     */
    public static PrivateKey getPrivateKeyFromHex(String hexPrivateKey) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException, IllegalArgumentException {
        if (hexPrivateKey == null || hexPrivateKey.isEmpty()) {
            throw new IllegalArgumentException("Private key hex string cannot be null or empty");
        }
        KeyFactory keyFactory = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
        byte[] decodedKey = Hex.decode(hexPrivateKey);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKey);
        return keyFactory.generatePrivate(keySpec);
    }

    /**
     * SM2 加密 (输入为字节数组)
     * @param data 待加密数据
     * @param publicKey 公钥
     * @return Base64编码的加密字符串
     */
    public static String encryptSM2(byte[] data, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("SM2", BouncyCastleProvider.PROVIDER_NAME);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return Base64.getEncoder().encodeToString(cipher.doFinal(data));
    }
    
    /**
     * SM2 加密 (输入为字符串)
     * @param data 待加密数据
     * @param publicKeyHex 十六进制公钥
     * @return Base64编码的加密字符串
     */
    public static String encryptSM2(String data, String publicKeyHex) throws Exception {
        PublicKey publicKey = getPublicKeyFromHex(publicKeyHex);
        return encryptSM2(data.getBytes(StandardCharsets.UTF_8), publicKey);
    }

    /**
     * SM2 解密 (输出为字节数组)
     * @param encryptedData Base64编码的加密数据
     * @param privateKey 私钥
     * @return 解密后的字节数组
     */
    public static byte[] decryptSM2(String encryptedData, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("SM2", BouncyCastleProvider.PROVIDER_NAME);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(Base64.getDecoder().decode(encryptedData));
    }
    
    /**
     * SM2 解密 (输出为字符串)
     * @param encryptedData Base64编码的加密数据
     * @param privateKeyHex 十六进制私钥
     * @return 解密后的字符串
     */
    public static String decryptSM2(String encryptedData, String privateKeyHex) throws Exception {
        if (encryptedData == null) {
            return null; // 如果输入为空，则直接返回null
        }
        if (privateKeyHex == null || privateKeyHex.isEmpty()) {
            throw new IllegalArgumentException("Private key hex string cannot be null or empty");
        }
        PrivateKey privateKey = getPrivateKeyFromHex(privateKeyHex);
        byte[] decryptedBytes = decryptSM2(encryptedData, privateKey);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }


    // --- SM3 (哈希算法) ---

    /**
     * 生成SM3哈希值。
     * @param data 原始数据
     * @return 十六进制哈希字符串
     */
    public static String generateSM3Hash(String data) throws NoSuchAlgorithmException, NoSuchProviderException {
        MessageDigest md = MessageDigest.getInstance("SM3", BouncyCastleProvider.PROVIDER_NAME);
        byte[] hashBytes = md.digest(data.getBytes(StandardCharsets.UTF_8));
        return Hex.toHexString(hashBytes);
    }


    // --- SM4 (对称加密) ---

    /**
     * SM4 加密。
     * @param data 原始数据
     * @param keyHex 十六进制密钥 (128位, 32个十六进制字符)
     * @return Base64编码的加密字符串
     */
    public static String encryptSM4(String data, String keyHex) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(Hex.decode(keyHex), "SM4");
        Cipher cipher = Cipher.getInstance("SM4/ECB/PKCS5Padding", BouncyCastleProvider.PROVIDER_NAME);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * SM4 解密。
     * @param encryptedData Base64编码的加密数据
     * @param keyHex 十六进制密钥 (128位, 32个十六进制字符)
     * @return 原始数据
     */
    public static String decryptSM4(String encryptedData, String keyHex) throws Exception {
        if (encryptedData == null) {
            return null; // 如果输入为空，则直接返回null
        }
        if (keyHex == null || keyHex.isEmpty()) {
            throw new IllegalArgumentException("SM4 key hex string cannot be null or empty");
        }
        SecretKeySpec secretKey = new SecretKeySpec(Hex.decode(keyHex), "SM4");
        Cipher cipher = Cipher.getInstance("SM4/ECB/PKCS5Padding", BouncyCastleProvider.PROVIDER_NAME);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] originalBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(originalBytes, StandardCharsets.UTF_8);
    }


    /**
     * 主方法，用于生成新的SM2密钥对并以十六进制格式打印。
     */
    public static void main(String[] args) throws Exception {
        // 生成并打印 SM2 密钥对
        KeyPair keyPair = generateSM2KeyPair();
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        String publicKeyHex = Hex.toHexString(publicKey.getEncoded());
        String privateKeyHex = Hex.toHexString(privateKey.getEncoded());

        System.out.println("--- Generated SM2 Key Pair (Hex Encoded) ---");
        System.out.println("New SM2 Public Key (Hex):");
        System.out.println(publicKeyHex);
        System.out.println("\nNew SM2 Private Key (Hex):");
        System.out.println(privateKeyHex);
        System.out.println("\n--- Please update these keys in db.properties ---");
        
        // 生成一个随机的 SM4 密钥
        byte[] sm4KeyBytes = new byte[16];
        new SecureRandom().nextBytes(sm4KeyBytes);
        String sm4KeyHex = Hex.toHexString(sm4KeyBytes);
        System.out.println("\n--- Generated SM4 Key (Hex Encoded) ---");
        System.out.println("New SM4 Key (Hex):");
        System.out.println(sm4KeyHex);
        System.out.println("\n--- Please update this key in db.properties ---");
    }
}
