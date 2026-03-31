package com.alonzo.citeval.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

@Service
public class KeyService {

    private PrivateKey serverPrivateKey;
    private PublicKey serverPublicKey;

    private static final String PRIVATE_KEY_FILE = "server_private.key";
    private static final String PUBLIC_KEY_FILE = "server_public.key";

    @PostConstruct
    public void init() {
        try {
            File privFile = new File(PRIVATE_KEY_FILE);
            File pubFile = new File(PUBLIC_KEY_FILE);

            if (privFile.exists() && pubFile.exists()) {
                KeyFactory kf = KeyFactory.getInstance("EC");

                byte[] privBytes = Files.readAllBytes(privFile.toPath());
                serverPrivateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privBytes));

                byte[] pubBytes = Files.readAllBytes(pubFile.toPath());
                serverPublicKey = kf.generatePublic(new X509EncodedKeySpec(pubBytes));
            } else {
                generateNewKeys();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize keys", e);
        }
    }

    private void generateNewKeys() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair kp = kpg.generateKeyPair();

        serverPrivateKey = kp.getPrivate();
        serverPublicKey = kp.getPublic();

        Files.write(new File(PRIVATE_KEY_FILE).toPath(), serverPrivateKey.getEncoded());
        Files.write(new File(PUBLIC_KEY_FILE).toPath(), serverPublicKey.getEncoded());
    }

    public PrivateKey getServerPrivateKey() {
        return serverPrivateKey;
    }

    public PublicKey getServerPublicKey() {
        return serverPublicKey;
    }
}
