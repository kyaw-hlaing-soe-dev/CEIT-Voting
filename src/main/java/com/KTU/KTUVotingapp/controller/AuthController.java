package com.KTU.KTUVotingapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/verify-pin")
    public ResponseEntity<?> deprecatedVerifyPin() {
        return ResponseEntity.status(410).body(Map.of("message", "Use /api/pins/verify for 8-digit PIN verification"));
    }
}
