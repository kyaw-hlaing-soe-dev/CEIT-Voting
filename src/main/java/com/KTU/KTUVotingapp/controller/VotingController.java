package com.KTU.KTUVotingapp.controller;

import com.KTU.KTUVotingapp.dto.BulkVoteRequest;
import com.KTU.KTUVotingapp.dto.VoteRequest;
import com.KTU.KTUVotingapp.dto.VoteResponse;
import com.KTU.KTUVotingapp.model.Category;
import com.KTU.KTUVotingapp.service.VotingService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/voting")
@CrossOrigin(origins = "*")
public class VotingController {

    private final VotingService votingService;

    public VotingController(VotingService votingService) {
        this.votingService = votingService;
    }

    /**
     * Submit a single vote for a category.
     * Request: { "deviceId": "...", "pin": "12345", "category": "KING", "candidateNumber": 1 }
     * Response: { "success": true, "message": "Vote submitted successfully" }
     */
    @PostMapping("/vote")
    public ResponseEntity<VoteResponse> submitVote(@Valid @RequestBody VoteRequest request, HttpServletRequest httpRequest) {
        try {
            // Prefer server-side device cookie; otherwise derive from IP+UA hash
            String resolvedDeviceId = resolveDeviceId(httpRequest);
            if (resolvedDeviceId != null && !resolvedDeviceId.isBlank()) {
                request.setDeviceId(resolvedDeviceId);
            }
            // Set User-Agent and IP for tracking
            request.setUserAgent(getUserAgent(httpRequest));
            request.setIpAddress(getClientIpAddress(httpRequest));

            votingService.submitVote(request);
            return ResponseEntity.ok(new VoteResponse(true, "Vote submitted successfully"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(new VoteResponse(false, e.getReason()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new VoteResponse(false, "An error occurred while processing your vote"));
        }
    }

    /**
     * Submit multiple votes in a single transaction (bulk voting).
     * Request: { "deviceId": "...", "pin": "12345", "votes": [{ "category": "KING", "candidateNumber": 1 }, ...] }
     */
    @PostMapping("/bulk-vote")
    public ResponseEntity<VoteResponse> submitBulkVotes(@Valid @RequestBody BulkVoteRequest request, HttpServletRequest httpRequest) {
        try {
            // Prefer server-side device cookie; otherwise derive from IP+UA hash
            String resolvedDeviceId = resolveDeviceId(httpRequest);
            if (resolvedDeviceId != null && !resolvedDeviceId.isBlank()) {
                request.setDeviceId(resolvedDeviceId);
            }
            // Set User-Agent and IP for tracking
            request.setUserAgent(getUserAgent(httpRequest));
            request.setIpAddress(getClientIpAddress(httpRequest));

            votingService.submitBulkVotes(request);
            return ResponseEntity.ok(new VoteResponse(true, "All votes submitted successfully"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(new VoteResponse(false, e.getReason()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new VoteResponse(false, "An error occurred while processing your votes"));
        }
    }

    /**
     * Check if a PIN has voted in a specific category.
     * GET /api/voting/has-voted?pin=12345&category=KING
     */
    @GetMapping("/has-voted")
    public ResponseEntity<Boolean> hasVoted(
            @RequestParam String pin,
            @RequestParam String category) {
        try {
            Category categoryEnum = Category.valueOf(category.toUpperCase());
            boolean hasVoted = votingService.hasVoted(pin, categoryEnum);
            return ResponseEntity.ok(hasVoted);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(false);
        }
    }

    /**
     * Check if a device has voted.
     * GET /api/voting/device-has-voted?deviceId=...
     */
    @GetMapping("/device-has-voted")
    public ResponseEntity<Boolean> deviceHasVoted(@RequestParam String deviceId) {
        boolean hasVoted = votingService.deviceHasVoted(deviceId);
        return ResponseEntity.ok(hasVoted);
    }

    /**
     * Check if an IP address has already voted.
     * GET /api/voting/ip-has-voted?ip=... or auto-detect from request
     */
    @GetMapping("/ip-has-voted")
    public ResponseEntity<Boolean> ipHasVoted(
            @RequestParam(required = false) String ip,
            HttpServletRequest httpRequest) {
        String ipToCheck = (ip != null && !ip.isBlank()) ? ip : getClientIpAddress(httpRequest);
        boolean hasVoted = votingService.ipHasVoted(ipToCheck);
        return ResponseEntity.ok(hasVoted);
    }

    /**
     * Resolve device ID using IP + User-Agent hash.
     * This helps identify unique devices even on the same network.
     */
    private String resolveDeviceId(HttpServletRequest request) {
        // Use IP + User-Agent based ID for better device fingerprinting
        return deriveDeviceId(request);
    }

    /**
     * Derive device ID from IP address and User-Agent.
     * This helps distinguish different devices on the same network.
     */
    private String deriveDeviceId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = getClientIpAddress(request);
        String userAgent = getUserAgent(request);

        // Combine IP and User-Agent for better device fingerprinting
        String source = (ip == null ? "unknown" : ip) + "|" + (userAgent == null ? "unknown" : userAgent);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 16; i++) {
                sb.append(String.format("%02x", hash[i]));
            }
            return "dev-" + sb;
        } catch (NoSuchAlgorithmException e) {
            return "dev-" + ip.replace(".", "-").replace(":", "-");
        }
    }

    /**
     * Get User-Agent header from request for device fingerprinting.
     */
    private String getUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isBlank()) {
            return "unknown";
        }
        return userAgent.trim();
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * Handle validation errors and return detailed error messages.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<VoteResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(new VoteResponse(false, "Validation failed: " + errors));
    }

    /**
     * Handle JSON parsing errors (e.g., invalid enum values).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<VoteResponse> handleJsonParseException(HttpMessageNotReadableException ex) {
        String message = "Invalid request format";
        Throwable cause = ex.getCause();
        if (cause != null && cause.getMessage() != null) {
            message = cause.getMessage();
        }
        return ResponseEntity.badRequest()
                .body(new VoteResponse(false, message));
    }
}

