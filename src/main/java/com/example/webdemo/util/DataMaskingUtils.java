package com.example.webdemo.util;

public class DataMaskingUtils {

    /**
     * Masks the name. For 2-character names, adds * in the middle.
     * For names longer than 2 characters, replaces all middle characters with *.
     * Example: "张三" -> "张*三", "李小明" -> "李*明", "欧阳娜娜" -> "欧**娜"
     *
     * @param name The original name.
     * @return The masked name.
     */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        if (name.length() == 1) {
            return name; // Or return "*" if single char names should also be masked
        }
        if (name.length() == 2) {
            return name.charAt(0) + "*" + name.charAt(1);
        }
        // For names with more than 2 characters, keep first and last, replace middle with *
        // To match the example "欧阳娜娜" -> "欧**娜" (length 4, 2 stars)
        // or "李小明" -> "李*明" (length 3, 1 star)
        // The number of stars should be name.length() - 2
        StringBuilder maskedName = new StringBuilder();
        maskedName.append(name.charAt(0));
        for (int i = 0; i < name.length() - 2; i++) {
            maskedName.append('*');
        }
        maskedName.append(name.charAt(name.length() - 1));
        return maskedName.toString();
    }

    /**
     * Masks the ID card number. Replaces characters from index 6 to 13 (inclusive, 8 chars) with *.
     * Example: "340123199001011234" -> "340123********1234"
     *
     * @param idCard The original ID card number.
     * @return The masked ID card number.
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 15) { // Basic check for typical ID length
            return idCard; // Or return a generic masked string like "**************"
        }
        // As per requirement: "身份证号(其中出示日期用*代替)" - assuming birth date part
        // For an 18-digit ID, birth date is usually from index 6 to 13 (YYYYMMDD)
        int startIndex = 6;
        int endIndex = 13; // exclusive for substring, so up to 13

        if (idCard.length() < endIndex +1 ) { // if id card is shorter than expected for masking this range
            startIndex = Math.min(startIndex, idCard.length()-1);
            endIndex = Math.min(endIndex, idCard.length()-1);
            if (startIndex >= endIndex && idCard.length() > 2) {
                 startIndex = idCard.length() / 3;
                 endIndex = startIndex * 2;
            }
        }

        StringBuilder masked = new StringBuilder(idCard.substring(0, startIndex));
        for (int i = startIndex; i <= endIndex && i < idCard.length(); i++) {
            masked.append('*');
        }
        if (endIndex + 1 < idCard.length()){
            masked.append(idCard.substring(endIndex + 1));
        }
        return masked.toString();
    }

    /**
     * Masks the phone number. Replaces characters from index 3 to 6 (inclusive, 4 chars) with *.
     * Example: "13812345678" -> "138****5678"
     *
     * @param phoneNumber The original phone number.
     * @return The masked phone number.
     */
    public static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() != 11) { // Assuming 11-digit phone numbers
            return phoneNumber; // Or return a generic masked string
        }
        return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(7);
    }

    public static void main(String[] args) {
        System.out.println("Masked Name (张三): " + maskName("张三"));
        System.out.println("Masked Name (李小明): " + maskName("李小明"));
        System.out.println("Masked Name (欧阳娜娜): " + maskName("欧阳娜娜"));
        System.out.println("Masked Name (王): " + maskName("王"));

        System.out.println("Masked ID (340123199001011234): " + maskIdCard("340123199001011234"));
        System.out.println("Masked ID (short 1234567): " + maskIdCard("1234567"));


        System.out.println("Masked Phone (13812345678): " + maskPhoneNumber("13812345678"));
        System.out.println("Masked Phone (invalid): " + maskPhoneNumber("138123456"));
    }
}
