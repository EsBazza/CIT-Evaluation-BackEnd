package com.alonzo.citeval.controller;

import com.alonzo.citeval.service.KeyService;
import org.springframework.web.bind.annotation.*;
import java.util.Base64;

@RestController
@RequestMapping("/api/keys")
public class KeyController {

    private final KeyService keyService;

    public KeyController(KeyService keyService) {
        this.keyService = keyService;
    }

    @GetMapping("/handshake")
    public String handshake() {
        return Base64.getEncoder().encodeToString(keyService.getServerPublicKey().getEncoded());
    }
}
