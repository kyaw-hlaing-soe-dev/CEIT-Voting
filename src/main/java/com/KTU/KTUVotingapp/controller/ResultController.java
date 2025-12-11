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
}

