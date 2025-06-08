package com.example.webdemo.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class QRCodeUtils {

    /**
     * Generates a QR code image from the given text and returns it as a Base64 encoded string.
     *
     * @param text The text to encode in the QR code.
     * @param width The desired width of the QR code image.
     * @param height The desired height of the QR code image.
     * @return Base64 encoded string of the QR code image (PNG format), or null if generation fails.
     */
    public static String generateQRCodeBase64(String text, int width, int height) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L); // Error correction level
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1); // Margin around QR code

            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();
            return Base64.getEncoder().encodeToString(pngData);
        } catch (WriterException | IOException e) {
            System.err.println("Error generating QR code: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // public static void main(String[] args) {
    //     String data = "Hello, QR Code!\nName: 测试用户\nID: ************1234\nTime: 2024-01-01 10:00";
    //     String base64Image = generateQRCodeBase64(data, 300, 300);
    //     if (base64Image != null) {
    //         System.out.println("QR Code Base64 (copy and paste in a browser's address bar with 'data:image/png;base64,' prefix):");
    //         System.out.println("data:image/png;base64," + base64Image);
    //     } else {
    //         System.out.println("Failed to generate QR code.");
    //     }
    // }
}
