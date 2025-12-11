package com.KTU.KTUVotingapp.controller;

import com.KTU.KTUVotingapp.dto.BulkVoteRequest;
import com.KTU.KTUVotingapp.dto.VoteRequest;
import com.KTU.KTUVotingapp.dto.VoteResponse;
import com.KTU.KTUVotingapp.model.Category;
import com.KTU.KTUVotingapp.service.VotingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<VoteResponse> submitVote(@Valid @RequestBody VoteRequest request) {
        try {
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
    public ResponseEntity<VoteResponse> submitBulkVotes(@Valid @RequestBody BulkVoteRequest request) {
        try {
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
}

