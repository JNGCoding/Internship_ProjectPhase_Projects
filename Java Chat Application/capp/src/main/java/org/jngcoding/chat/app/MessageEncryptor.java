package org.jngcoding.chat.app;

public class MessageEncryptor {
    public static String encrypt(String original, int shift) {
        StringBuilder encrypted = new StringBuilder("");

        for (char character : original.toCharArray()) {
            if (Character.isLetter(character)) {
                char base = Character.isUpperCase(character) ? 'A' : 'a';
                encrypted.append((char) ((character - base + shift) % 26 + base));
            } else {
                encrypted.append(character);
            }
        }

        return encrypted.toString();
    }

    public static String decrypt(String encrypted, int shift) {
        return encrypt(encrypted, 26 - (shift % 26));
    }
}
