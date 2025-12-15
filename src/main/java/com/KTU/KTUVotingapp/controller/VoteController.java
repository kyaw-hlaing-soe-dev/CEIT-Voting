package com.KTU.KTUVotingapp.controller;

import com.KTU.KTUVotingapp.model.Category;
import com.KTU.KTUVotingapp.model.Vote;
import com.KTU.KTUVotingapp.service.VoteService;
import com.KTU.KTUVotingapp.exception.InvalidVoteException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/votes")
public class VoteController {

    private final VoteService voteService;

    public static record VoteRequest(Long voterId, String category, Integer candidateNumber) {}

    public VoteController(VoteService voteService) { this.voteService = voteService; }

    @PostMapping
    public ResponseEntity<?> castVote(@RequestBody VoteRequest req) {
        try {
            Category cat = Category.valueOf(req.category());
            Vote saved = voteService.castOrUpdateVote(req.voterId(), cat, req.candidateNumber());
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Unknown category: " + req.category());
        } catch (InvalidVoteException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
