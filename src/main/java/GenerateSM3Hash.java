import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;

/**
 * 生成 SM3 密码哈希的简单示例
 */
public class GenerateSM3Hash {
    public static void main(String[] args) {
        try {
            // 添加 BouncyCastle 作为安全提供者
            Security.addProvider(new BouncyCastleProvider());
            
            String password = "admin123";
            
            // 使用 SM3 算法创建 MessageDigest
            MessageDigest digest = MessageDigest.getInstance("SM3", "BC");
            
            // 计算哈希
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            
            // 转换为十六进制字符串
            String hexHash = Hex.toHexString(hash);
            
            System.out.println("原始密码: " + password);
            System.out.println("SM3 哈希值: " + hexHash);
            System.out.println("哈希长度: " + hexHash.length() + " 字符");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
