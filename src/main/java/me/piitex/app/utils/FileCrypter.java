package me.piitex.app.utils;


import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

/**
 * Utility used for encrypted all data. Privacy is taken very seriously. To further take this seriously please change the pass key.
 * <p>
 * The algorithm used is AES. The security key is 'PBKDF2WithHmacSHA256'. The pass key is uniquely set based on the pc.
 *<p>
 * Originally created but improved with AI.
 * AI added salt randomization, randomized pass key, and other various improvements.
 */
public class FileCrypter {
    private static final String PBKDF_ALG = "PBKDF2WithHmacSHA256";
    private static final int KEY_LENGTH_BITS = 256;
    private static final int ITERATION_COUNT = 1000000;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    // NOT THE CORRECT WAY TO DO THIS
    // Key-wrapping should be used instead.
    private static final String passKey = System.getProperty("user.name") + System.getProperty("user.home") + System.getProperty("os.name");

    public static void encryptFile(File file, File outputFile) {
        SecureRandom secureRandom = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        secureRandom.nextBytes(salt); // Generate a unique salt for each encryption

        byte[] iv = new byte[GCM_IV_LENGTH_BYTES]; // Generate a unique IV for each encryption
        secureRandom.nextBytes(iv);

        try (FileInputStream inputStream = new FileInputStream(file);
             FileOutputStream outputStream = new FileOutputStream(outputFile)) {

            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF_ALG);
            char[] passKeyChars = passKey.toCharArray();
            KeySpec spec = new PBEKeySpec(passKeyChars, salt, ITERATION_COUNT, KEY_LENGTH_BITS);
            SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
            java.util.Arrays.fill(passKeyChars, (char) 0);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); // Use AES in GCM mode with no padding
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv); // Specify IV and tag length
            cipher.init(Cipher.ENCRYPT_MODE, secret, gcmSpec);

            // Write salt and IV to the output file
            outputStream.write(salt);
            outputStream.write(iv);

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byte[] output = cipher.update(buffer, 0, bytesRead);
                if (output != null) {
                    outputStream.write(output);
                }
            }
            byte[] outputBytes = cipher.doFinal(); // Finalize encryption and get authentication tag
            if (outputBytes != null) {
                outputStream.write(outputBytes);
            }

        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException |
                 InvalidKeyException | InvalidAlgorithmParameterException |
                 IllegalBlockSizeException | BadPaddingException | IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error encrypting file: " + e.getMessage(), e);
        }
    }

    public static void decryptFile(File file, File outputFile) throws IllegalBlockSizeException, IOException {
        try (FileInputStream inputStream = new FileInputStream(file);
             FileOutputStream outputStream = new FileOutputStream(outputFile)) {

            byte[] salt = new byte[SALT_LENGTH_BYTES];
            int bytesReadSalt = inputStream.read(salt);
            if (bytesReadSalt != SALT_LENGTH_BYTES) {
                throw new IOException("Could not read full salt from the encrypted file. Expected " + SALT_LENGTH_BYTES + " bytes, got " + bytesReadSalt);
            }

            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            int bytesReadIv = inputStream.read(iv);
            if (bytesReadIv != GCM_IV_LENGTH_BYTES) {
                throw new IOException("Could not read full IV from the encrypted file. Expected " + GCM_IV_LENGTH_BYTES + " bytes, got " + bytesReadIv);
            }

            char[] passKeyChars = passKey.toCharArray();
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF_ALG);
            KeySpec spec = new PBEKeySpec(passKeyChars, salt, ITERATION_COUNT, KEY_LENGTH_BITS);
            SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
            java.util.Arrays.fill(passKeyChars, (char) 0);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); // Use AES in GCM mode with no padding
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv); // Specify IV and tag length
            cipher.init(Cipher.DECRYPT_MODE, secret, gcmSpec);

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byte[] output = cipher.update(buffer, 0, bytesRead);
                if (output != null) {
                    outputStream.write(output);
                }
            }
            byte[] outputBytes = cipher.doFinal(); // Finalize decryption and verify authentication tag
            if (outputBytes != null) {
                outputStream.write(outputBytes);
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | BadPaddingException e) {
            throw new RuntimeException("Error decrypting file: " + e.getMessage(), e);
        }
    }
}