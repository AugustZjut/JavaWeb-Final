package com.example.webdemo.util;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
// Added imports for SM4 and SM2
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.BadPaddingException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class CryptoUtils {

    static {
        // Add Bouncy Castle as a security provider
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * Generates SM3 hash for the input string.
     *
     * @param input The string to hash.
     * @return The SM3 hash projetos a hex string, or null if an error occurs.
     */
    public static String generateSM3Hash(String input) {
        if (input == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SM3", BouncyCastleProvider.PROVIDER_NAME);
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Hex.toHexString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("SM3 algorithm not found: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("Error generating SM3 hash: " + e.getMessage());
            return null;
        }
    }

    /**
     * Encrypts data using SM2 algorithm.
     * Assumes publicKeyString is a hex representation of a DER-encoded X.509 public key.
     */
    public static String encryptSM2(String data, String publicKeyString) {
        if (data == null || publicKeyString == null) {
            return null;
        }
        try {
            byte[] publicKeyBytes = Hex.decode(publicKeyString);
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
            PublicKey publicKey = keyFactory.generatePublic(x509KeySpec);

            Cipher cipher = Cipher.getInstance("SM2", BouncyCastleProvider.PROVIDER_NAME);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Hex.toHexString(encryptedBytes);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 IllegalBlockSizeException | BadPaddingException | InvalidKeySpecException e) {
            System.err.println("SM2 Encryption error: " + e.getMessage());
            e.printStackTrace(); // For detailed debugging
            return null;
        } catch (Exception e) {
            System.err.println("Unexpected error during SM2 encryption: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Decrypts data using SM2 algorithm.
     * Assumes privateKeyString is a hex representation of a DER-encoded PKCS#8 private key.
     */
    public static String decryptSM2(String encryptedDataHex, String privateKeyString) {
        if (encryptedDataHex == null || privateKeyString == null) {
            return null;
        }
        try {
            byte[] privateKeyBytes = Hex.decode(privateKeyString);
            PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
            PrivateKey privateKey = keyFactory.generatePrivate(pkcs8KeySpec);

            Cipher cipher = Cipher.getInstance("SM2", BouncyCastleProvider.PROVIDER_NAME);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedBytes = cipher.doFinal(Hex.decode(encryptedDataHex));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 IllegalBlockSizeException | BadPaddingException | InvalidKeySpecException e) {
            System.err.println("SM2 Decryption error: " + e.getMessage());
            e.printStackTrace(); // For detailed debugging
            return null;
        } catch (Exception e) {
            System.err.println("Unexpected error during SM2 decryption: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Encrypts data using SM4 algorithm (ECB mode).
     * keyString must be a 32-character hex string (128-bit key).
     */
    public static String encryptSM4(String data, String keyString) {
        if (data == null || keyString == null) {
            return null;
        }
        try {
            byte[] keyBytes = Hex.decode(keyString);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "SM4");
            // BouncyCastle provider is needed for "SM4/ECB/PKCS5Padding" or "SM4" if default JCE doesn't have it
            // Using "SM4/ECB/PKCS7Padding" as BouncyCastle often uses PKCS7 for PKCS5
            Cipher cipher = Cipher.getInstance("SM4/ECB/PKCS7Padding", BouncyCastleProvider.PROVIDER_NAME);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Hex.toHexString(encryptedBytes);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 IllegalBlockSizeException | BadPaddingException e) {
            System.err.println("SM4 Encryption error: " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            System.err.println("Unexpected error during SM4 encryption: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Decrypts data using SM4 algorithm (ECB mode).
     * keyString must be a 32-character hex string (128-bit key).
     */
    public static String decryptSM4(String encryptedDataHex, String keyString) {
        if (encryptedDataHex == null || keyString == null) {
            return null;
        }
        try {
            byte[] keyBytes = Hex.decode(keyString);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "SM4");
            Cipher cipher = Cipher.getInstance("SM4/ECB/PKCS7Padding", BouncyCastleProvider.PROVIDER_NAME);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Hex.decode(encryptedDataHex));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 IllegalBlockSizeException | BadPaddingException e) {
            System.err.println("SM4 Decryption error: " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            System.err.println("Unexpected error during SM4 decryption: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Helper method to generate an SM2 KeyPair
    public static KeyPair generateSM2KeyPair() throws NoSuchAlgorithmException, java.security.spec.InvalidParameterSpecException, java.security.InvalidAlgorithmParameterException, java.security.NoSuchProviderException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
        // Standard SM2 curve sm2p256v1
        kpg.initialize(new ECGenParameterSpec("sm2p256v1"), new SecureRandom());
        return kpg.generateKeyPair();
    }

    // Helper method to generate an SM4 Key (128 bit)
    public static SecretKey generateSM4Key() throws NoSuchAlgorithmException, java.security.NoSuchProviderException {
        KeyGenerator keyGen = KeyGenerator.getInstance("SM4", BouncyCastleProvider.PROVIDER_NAME);
        keyGen.init(128, new SecureRandom()); // 128 bits
        return keyGen.generateKey();
    }


    public static void main(String[] args) {
        // Test SM3
        String originalPassword = "TestPassword123!";
        String sm3Hash = generateSM3Hash(originalPassword);
        System.out.println("Original Password: " + originalPassword);
        System.out.println("SM3 Hash: " + sm3Hash);
        System.out.println("SM3 Hash Length (Hex): " + (sm3Hash != null ? sm3Hash.length() : 0));

        // Test SM4 Key Generation and Encryption/Decryption
        try {
            SecretKey sm4Key = generateSM4Key();
            String sm4KeyHex = Hex.toHexString(sm4Key.getEncoded());
            System.out.println("\nGenerated SM4 Key (Hex): " + sm4KeyHex);
            System.out.println("SM4 Key Length (Hex): " + sm4KeyHex.length());

            String originalDataSM4 = "This is a secret message for SM4.";
            System.out.println("Original Data for SM4: " + originalDataSM4);

            String encryptedSM4 = encryptSM4(originalDataSM4, sm4KeyHex);
            System.out.println("Encrypted SM4 (Hex): " + encryptedSM4);

            String decryptedSM4 = decryptSM4(encryptedSM4, sm4KeyHex);
            System.out.println("Decrypted SM4: " + decryptedSM4);

        } catch (NoSuchAlgorithmException | java.security.NoSuchProviderException e) {
            System.err.println("Error in SM4 test: " + e.getMessage());
            e.printStackTrace();
        }

        // Test SM2 Key Generation and Encryption/Decryption
        try {
            KeyPair sm2KeyPair = generateSM2KeyPair();
            PublicKey sm2PublicKey = sm2KeyPair.getPublic();
            PrivateKey sm2PrivateKey = sm2KeyPair.getPrivate();

            String sm2PublicKeyHex = Hex.toHexString(sm2PublicKey.getEncoded());
            String sm2PrivateKeyHex = Hex.toHexString(sm2PrivateKey.getEncoded());

            System.out.println("\nGenerated SM2 Public Key (Hex): " + sm2PublicKeyHex);
            System.out.println("SM2 Public Key Length (Hex): " + sm2PublicKeyHex.length());
            System.out.println("Generated SM2 Private Key (Hex): " + sm2PrivateKeyHex);
            System.out.println("SM2 Private Key Length (Hex): " + sm2PrivateKeyHex.length());

            String originalDataSM2 = "This is a very confidential message for SM2.";
            System.out.println("Original Data for SM2: " + originalDataSM2);

            String encryptedSM2 = encryptSM2(originalDataSM2, sm2PublicKeyHex);
            System.out.println("Encrypted SM2 (Hex): " + encryptedSM2);

            String decryptedSM2 = decryptSM2(encryptedSM2, sm2PrivateKeyHex);
            System.out.println("Decrypted SM2: " + decryptedSM2);

        } catch (NoSuchAlgorithmException | java.security.spec.InvalidParameterSpecException | java.security.InvalidAlgorithmParameterException | java.security.NoSuchProviderException e) {
            System.err.println("Error in SM2 test: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
