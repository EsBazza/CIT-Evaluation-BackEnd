package com.alonzo.citeval.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
public class KeyService {

    private PrivateKey serverPrivateKey;
    private PublicKey serverPublicKey;

    private final Path privateKeyPath;
    private final Path publicKeyPath;
    private final String privateKeyBase64;
    private final String publicKeyBase64;

    public KeyService(@Value("${app.keys.private-file:server_private.key}") String privateKeyFile,
                      @Value("${app.keys.public-file:server_public.key}") String publicKeyFile,
                      @Value("${app.keys.private-base64:}") String privateKeyBase64,
                      @Value("${app.keys.public-base64:}") String publicKeyBase64) {
        this.privateKeyPath = Path.of(privateKeyFile);
        this.publicKeyPath = Path.of(publicKeyFile);
        this.privateKeyBase64 = privateKeyBase64;
        this.publicKeyBase64 = publicKeyBase64;
    }

    @PostConstruct
    public void init() {
        try {
            if (hasText(privateKeyBase64) && hasText(publicKeyBase64)) {
                loadKeysFromBase64();
                return;
            }

            File privFile = privateKeyPath.toFile();
            File pubFile = publicKeyPath.toFile();

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

    private void loadKeysFromBase64() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("EC");
        byte[] privBytes = Base64.getDecoder().decode(privateKeyBase64.trim());
        byte[] pubBytes = Base64.getDecoder().decode(publicKeyBase64.trim());

        serverPrivateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privBytes));
        serverPublicKey = kf.generatePublic(new X509EncodedKeySpec(pubBytes));
    }

    private void generateNewKeys() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair kp = kpg.generateKeyPair();

        serverPrivateKey = kp.getPrivate();
        serverPublicKey = kp.getPublic();

        ensureParentDirectoryExists(privateKeyPath);
        ensureParentDirectoryExists(publicKeyPath);

        Files.write(privateKeyPath, serverPrivateKey.getEncoded());
        Files.write(publicKeyPath, serverPublicKey.getEncoded());
    }

    private void ensureParentDirectoryExists(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public PrivateKey getServerPrivateKey() {
        return serverPrivateKey;
    }

    public PublicKey getServerPublicKey() {
        return serverPublicKey;
    }
}
