package com.KTU.KTUVotingapp.controller;

import com.KTU.KTUVotingapp.dto.VerifyPinRequest;
import com.KTU.KTUVotingapp.dto.VerifyPinResponse;
import com.KTU.KTUVotingapp.model.Voter;
import com.KTU.KTUVotingapp.repository.VoterRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final VoterRepository voterRepository;

    public AuthController(VoterRepository voterRepository) {
        this.voterRepository = voterRepository;
    }

    /**
     * Verify that the given PIN exists.
     * For shared PINs (one PIN for all users), this just verifies the PIN is valid.
     * Device-specific voting status is checked when votes are submitted.
     * Returns 200 with status details, or 404 if the PIN is unknown.
     */
    @PostMapping("/verify-pin")
    public ResponseEntity<VerifyPinResponse> verifyPin(@Valid @RequestBody VerifyPinRequest request) {
        // For shared PINs, we just check if any voter exists with this PIN
        // The actual device-specific voting check happens during vote submission
        boolean pinExists = voterRepository.existsByPin(request.getPin());
        
        if (!pinExists) {
            return ResponseEntity.status(404).body(new VerifyPinResponse(false, false));
        }

        // PIN exists - return valid (device-specific voting status checked later)
        return ResponseEntity.ok(new VerifyPinResponse(true, false));
    }
}
