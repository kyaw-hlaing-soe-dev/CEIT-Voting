package com.KTU.KTUVotingapp.controller;

import com.KTU.KTUVotingapp.dto.CandidateDTO;
import com.KTU.KTUVotingapp.model.Category;
import com.KTU.KTUVotingapp.service.CandidateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/candidates")
@CrossOrigin(origins = "*")
public class CandidateController {

    private final CandidateService candidateService;

    public CandidateController(CandidateService candidateService) {
        this.candidateService = candidateService;
    }

    /**
     * Get all candidates for a specific category.
     * Categories: KING, QUEEN, PRINCE, PRINCESS, COUPLE
     */
    @GetMapping("/{category}")
    public ResponseEntity<List<CandidateDTO>> getCandidatesByCategory(@PathVariable String category) {
        try {
            Category categoryEnum = Category.valueOf(category.toUpperCase());
            List<CandidateDTO> candidates = candidateService.getCandidatesByCategory(categoryEnum);
            return ResponseEntity.ok(candidates);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

