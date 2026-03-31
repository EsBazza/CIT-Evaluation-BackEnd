package com.alonzo.citeval.util;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class CryptoUtils {

    public static String decrypt(String ciphertext, String studentPubKeyBase64, String ivBase64, PrivateKey serverPrivKey) throws Exception {
        // 1. Decode Student's Public Key
        byte[] studentPubKeyBytes = Base64.getDecoder().decode(studentPubKeyBase64);
        KeyFactory kf = KeyFactory.getInstance("EC");
        PublicKey studentPublicKey = kf.generatePublic(new X509EncodedKeySpec(studentPubKeyBytes));

        // 2. Derive Shared Secret
        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(serverPrivKey);
        ka.doPhase(studentPublicKey, true);
        byte[] sharedSecret = ka.generateSecret();

        // --- ALIGNMENT FIX ---
        // React's subtle.deriveKey uses the raw secret. Java sometimes adds a leading 0.
        // We ensure we have exactly 32 bytes for AES-256.
        byte[] aesKeyBytes = new byte[32];
        if (sharedSecret.length > 32) {
            System.arraycopy(sharedSecret, sharedSecret.length - 32, aesKeyBytes, 0, 32);
        } else {
            System.arraycopy(sharedSecret, 0, aesKeyBytes, 32 - sharedSecret.length, sharedSecret.length);
        }
        SecretKeySpec aesKey = new SecretKeySpec(aesKeyBytes, "AES");
        // ---------------------

        byte[] iv = Base64.getDecoder().decode(ivBase64);
        byte[] decodedCiphertext = Base64.getDecoder().decode(ciphertext);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, spec);

        byte[] plainText = cipher.doFinal(decodedCiphertext);
        return new String(plainText);
    }
}
