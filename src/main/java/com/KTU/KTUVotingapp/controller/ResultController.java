package com.KTU.KTUVotingapp.controller;

import com.KTU.KTUVotingapp.dto.ResultDTO;
import com.KTU.KTUVotingapp.model.Category;
import com.KTU.KTUVotingapp.service.ResultService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/results")
@CrossOrigin(origins = "*")
public class ResultController {

    private final ResultService resultService;

    public ResultController(ResultService resultService) {
        this.resultService = resultService;
    }

    /**
     * Get voting results for a specific category.
     * GET /api/results/{category}
     * Categories: KING, QUEEN, PRINCE, PRINCESS, COUPLE
     */
    @GetMapping("/{category}")
    public ResponseEntity<ResultDTO> getResultsByCategory(@PathVariable String category) {
        try {
            Category categoryEnum = Category.valueOf(category.toUpperCase());
            ResultDTO results = resultService.getResultsByCategory(categoryEnum);
            return ResponseEntity.ok(results);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get voting results for all categories.
     * GET /api/results/all
     */
    @GetMapping("/all")
    public ResponseEntity<List<ResultDTO>> getAllResults() {
        List<ResultDTO> results = resultService.getAllResults();
        return ResponseEntity.ok(results);
    }

    /**
     * Live admin flattened candidate results for dashboard polling.
     * Legacy endpoint (renamed to avoid collision). Use /api/admin/results (AdminController) for real admin dashboard.
     * GET /api/admin/results-legacy?pin={pin}
     */
    @GetMapping("/api/admin/results-legacy")
    public ResponseEntity<java.util.List<ResultDTO.CandidateResultDTO>> getLiveAdminResultsLegacy(@RequestParam(value = "pin", required = false) String pin) {
        // Keep legacy behavior: allow a particular hardcoded pin '99999' for backward compatibility.
        if (pin == null || !pin.equals("99999")) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        }

        // Aggregate all candidates from all categories and sort by voteCount desc
        java.util.List<ResultDTO> all = resultService.getAllResults();
        java.util.List<ResultDTO.CandidateResultDTO> candidates = all.stream()
                .flatMap(r -> r.getCandidates().stream())
                .sorted(java.util.Comparator.comparingLong(ResultDTO.CandidateResultDTO::getVoteCount).reversed())
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(candidates);
    }
}
