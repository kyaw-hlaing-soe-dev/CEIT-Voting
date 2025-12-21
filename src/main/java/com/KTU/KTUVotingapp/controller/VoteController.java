package com.KTU.KTUVotingapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/votes")
@Deprecated
public class VoteController {

    @PostMapping
    public ResponseEntity<?> disabled() {
        return ResponseEntity.status(410).body("Use /api/voting endpoints with X-Vote-Token");
    }
}
