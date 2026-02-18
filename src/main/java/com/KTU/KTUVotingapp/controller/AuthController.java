package com.KTU.KTUVotingapp.controller;

import java.util.Map;

import com.KTU.KTUVotingapp.model.UserRole;
import com.KTU.KTUVotingapp.service.RateLimitService;
import com.KTU.KTUVotingapp.service.VotingService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Value("${voting.user-pin:}")
    private String userPin;

    @Value("${voting.admin-pin:}")
    private String adminPin;

    private final VotingService votingService;
    private final RateLimitService rateLimitService;

    public AuthController(VotingService votingService, RateLimitService rateLimitService) {
        this.votingService = votingService;
        this.rateLimitService = rateLimitService;
    }

    /**
     * Verify PIN and return the appropriate role (RBAC-based).
     * Uses Spring Security authorities (ROLE_ADMIN, ROLE_USER) instead of simple boolean.
     */
    @PostMapping("/verify-pin")
    public ResponseEntity<?> verifyPin(@RequestBody Map<String, Object> body,
                                       HttpServletRequest request,
                                       HttpServletResponse response) {
        if (body == null || !body.containsKey("pin") || body.get("pin") == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Missing pin"));
        }

        String clientIp = getClientIpAddress(request);
        RateLimitService.RateLimitResult rateLimitResult = rateLimitService.checkRateLimit(clientIp);
        if (!rateLimitResult.isAllowed()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of(
                    "message", "Too many attempts. Please try again in " + rateLimitResult.getRetryAfterSeconds() + " seconds.",
                    "retryAfter", rateLimitResult.getRetryAfterSeconds()
                ));
        }

        String pin = String.valueOf(body.get("pin")).trim();

        // Issue/read tracking cookie (used as our browser identifier)
        String cookieId = getOrCreateTrackingCookie(request, response);
        boolean cookieAlreadyVoted = votingService.deviceHasVoted(cookieId);
        boolean ipAlreadyVoted = votingService.ipHasVoted(clientIp);
        boolean alreadyVoted = cookieAlreadyVoted || ipAlreadyVoted;

        // RBAC-based authentication using Spring Security authorities
        if (adminPin != null && !adminPin.isEmpty() && pin.equals(adminPin)) {
            rateLimitService.recordAttempt(clientIp, true);

            return ResponseEntity.ok(Map.of(
                "valid", true,
                "alreadyVoted", false, // Admins can always access admin panel
                "role", UserRole.ROLE_ADMIN.name(),
                "authority", "ROLE_ADMIN",
                "voteWeight", UserRole.ROLE_ADMIN.getVoteWeight(),
                "deviceId", cookieId
            ));
        }
        
        if (userPin != null && !userPin.isEmpty() && pin.equals(userPin)) {
            rateLimitService.recordAttempt(clientIp, true);

            return ResponseEntity.ok(Map.of(
                "valid", true,
                "alreadyVoted", alreadyVoted,
                "role", UserRole.ROLE_USER.name(),
                "authority", "ROLE_USER",
                "voteWeight", UserRole.ROLE_USER.getVoteWeight(),
                "deviceId", cookieId,
                "remainingAttempts", rateLimitResult.getRemainingAttempts()
            ));
        }

        rateLimitService.recordAttempt(clientIp, false);
        RateLimitService.RateLimitResult updatedRateLimit = rateLimitService.checkRateLimit(clientIp);

        return ResponseEntity.status(404).body(Map.of(
            "message", "Pin not found",
            "remainingAttempts", updatedRateLimit.getRemainingAttempts()
        ));
    }

    /**
     * Check if current browser has already voted (POST version)
     */
    @PostMapping("/check-device")
    public ResponseEntity<?> checkDevice(@RequestBody(required = false) Map<String, Object> body,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        String cookieId = getOrCreateTrackingCookie(request, response);
        boolean hasVoted = votingService.deviceHasVoted(cookieId) || votingService.ipHasVoted(getClientIpAddress(request));
        return ResponseEntity.ok(Map.of(
            "deviceId", cookieId,
            "hasVoted", hasVoted
        ));
    }

    /**
     * Legacy GET endpoint for backward compatibility
     */
    @GetMapping("/check-device")
    public ResponseEntity<?> checkDeviceGet(HttpServletRequest request, HttpServletResponse response) {
        String cookieId = getOrCreateTrackingCookie(request, response);
        boolean hasVoted = votingService.deviceHasVoted(cookieId) || votingService.ipHasVoted(getClientIpAddress(request));
        return ResponseEntity.ok(Map.of(
            "deviceId", cookieId,
            "hasVoted", hasVoted
        ));
    }

    /**
     * Get or create a persistent tracking cookie. This is intentionally
     * limited to a server-issued UUID stored as an HTTP-only cookie to keep
     * identification lightweight and privacy-friendly.
     */
    private String getOrCreateTrackingCookie(HttpServletRequest request, HttpServletResponse response) {
        String cookieId = null;
        if (request != null && request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("voting_device_id".equals(cookie.getName())) {
                    cookieId = cookie.getValue();
                    break;
                }
            }
        }
        if (cookieId == null || cookieId.isBlank()) {
            cookieId = "cookie-" + java.util.UUID.randomUUID();
        }

        Cookie deviceCookie = new Cookie("voting_device_id", cookieId);
        deviceCookie.setMaxAge(365 * 24 * 60 * 60); // 1 year
        deviceCookie.setPath("/");
        deviceCookie.setHttpOnly(true);
        deviceCookie.setSecure(request != null && request.isSecure());
        response.addCookie(deviceCookie);

        return cookieId;
    }

    /**
     * Get client IP address considering proxy headers
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_CLIENT_IP"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can contain multiple IPs, get the first one
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }
}

