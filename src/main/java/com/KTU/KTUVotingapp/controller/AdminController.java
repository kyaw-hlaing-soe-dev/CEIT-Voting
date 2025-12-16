package com.KTU.KTUVotingapp.controller;

import com.KTU.KTUVotingapp.dto.ResultDTO;
import com.KTU.KTUVotingapp.exception.ResourceNotFoundException;
import com.KTU.KTUVotingapp.exception.RankingConflictException;
import com.KTU.KTUVotingapp.model.Candidate;
import com.KTU.KTUVotingapp.model.Category;
import com.KTU.KTUVotingapp.service.ResultService;
import com.KTU.KTUVotingapp.repository.CandidateRepository;
import com.KTU.KTUVotingapp.service.ImageStorageService;
import com.KTU.KTUVotingapp.dto.CandidateForm;
import com.KTU.KTUVotingapp.service.CandidateService;
import com.KTU.KTUVotingapp.repository.AdminActionAuditRepository;
import com.KTU.KTUVotingapp.model.AdminActionAudit;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final ResultService resultService;

    private String adminPin;

    // Inject repository directly to avoid costly/contextual lookups per request
    private final CandidateRepository candidateRepository;

    private final ImageStorageService imageStorageService;

    private final CandidateService candidateService;

    private final AdminActionAuditRepository auditRepository;

    private static final int MIN_CANDIDATE_NUMBER = 1;
    private static final int MAX_CANDIDATE_NUMBER = 10;

    private void validateCandidateNumberOrThrow(Integer number) {
        if (number == null) {
            throw new IllegalArgumentException("Candidate number is required");
        }
        if (number < MIN_CANDIDATE_NUMBER || number > MAX_CANDIDATE_NUMBER) {
            throw new IllegalArgumentException("Candidate number must be between " + MIN_CANDIDATE_NUMBER + " and " + MAX_CANDIDATE_NUMBER);
        }
    }

    public AdminController(ResultService resultService, CandidateRepository candidateRepository, ImageStorageService imageStorageService, CandidateService candidateService, AdminActionAuditRepository auditRepository) {
        this.resultService = resultService;
        this.adminPin = "99999";
        this.candidateRepository = candidateRepository;
        this.imageStorageService = imageStorageService;
        this.candidateService = candidateService;
        this.auditRepository = auditRepository;
    }

    /**
     * Returns aggregated vote counts in the shape:
     * {
     *   "KING": {"1": 10, "2": 5, ...},
     *   "QUEEN": {"1": 3, ...},
     *   ...
     * }
     * Requires adminPin query parameter for a basic auth check.
     */
    @GetMapping("/results")
    public ResponseEntity<?> getResults(@RequestParam("adminPin") String pin) {
        if (pin == null || !pin.equals(adminPin)) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        Map<String, Map<Integer, Long>> results = new LinkedHashMap<>();

        // For each category, collect counts for candidate numbers
        for (Category category : Category.values()) {
            ResultDTO categoryResults = resultService.getResultsByCategory(category);
            Map<Integer, Long> counts = new LinkedHashMap<>();
            
            for (ResultDTO.CandidateResultDTO candidate : categoryResults.getCandidates()) {
                counts.put(candidate.getCandidateNumber(), candidate.getVoteCount());
            }
            
            results.put(category.name(), counts);
        }

        return ResponseEntity.ok(results);
    }

    /**
     * Get detailed results for all categories (with percentages).
     * Requires adminPin query parameter for a basic auth check.
     */
    @GetMapping("/results/detailed")
    public ResponseEntity<?> getDetailedResults(@RequestParam("adminPin") String pin) {
        if (pin == null || !pin.equals(adminPin)) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        List<ResultDTO> results = resultService.getAllResults();
        return ResponseEntity.ok(results);
    }

    /**
     * Live admin flattened candidate results for dashboard polling.
     * GET /api/admin/results?adminPin=99999
     */
    @GetMapping(value = "/results", params = "adminPin")
    public ResponseEntity<Map<String, Object>> getLiveAdminResults(@RequestParam("adminPin") String pin) {
        if (pin == null || !pin.equals(adminPin)) {
            return ResponseEntity.status(403).build();
        }

        java.util.List<ResultDTO> all = resultService.getAllResults();
        java.util.List<ResultDTO.CandidateResultDTO> candidates = all.stream()
                .flatMap(r -> r.getCandidates().stream())
                .sorted(java.util.Comparator.comparingLong(ResultDTO.CandidateResultDTO::getVoteCount).reversed())
                .collect(java.util.stream.Collectors.toList());

        Map<String, Object> payload = new HashMap<>();
        payload.put("candidates", candidates);
        payload.put("stats", Map.of(
                "totalVotes", resultService.getTotalVotesCast(),
                "totalVoters", resultService.getTotalVoters()
        ));
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/candidates")
    public ResponseEntity<java.util.List<com.KTU.KTUVotingapp.dto.CandidateDTO>> getAllCandidates(@RequestParam("adminPin") String pin) {
        if (pin == null || !pin.equals(adminPin)) {
            return ResponseEntity.status(403).build();
        }

        java.util.List<com.KTU.KTUVotingapp.model.Candidate> list = candidateRepository.findAll();
        java.util.List<com.KTU.KTUVotingapp.dto.CandidateDTO> dtos = list.stream()
                .map(c -> new com.KTU.KTUVotingapp.dto.CandidateDTO(c.getId(), c.getCategory(), c.getCandidateNumber(), c.getName(), c.getDepartment(), c.getImageUrl(), c.getVoteCount()))
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PostMapping(value = "/candidates", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createCandidate(@RequestParam("adminPin") String pin,
                                             @RequestBody com.KTU.KTUVotingapp.dto.CandidateDTO dto) {
        if (pin == null || !pin.equals(adminPin)) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        Candidate candidate = new Candidate();
        candidate.setCategory(dto.getCategory());
        candidate.setCandidateNumber(dto.getCandidateNumber());
        candidate.setName(dto.getName());
        candidate.setDepartment(dto.getDepartment());
        candidate.setImageUrl(dto.getImageUrl());
        candidate.setVoteCount(dto.getVoteCount() != null ? dto.getVoteCount() : 0L);

        try {
            validateCandidateNumberOrThrow(dto.getCandidateNumber());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "INVALID_NUMBER", "message", ex.getMessage()));
        }

        try {
            Candidate saved = candidateService.createCandidateTransactional(candidate);
            candidateService.evictCaches();

            com.KTU.KTUVotingapp.dto.CandidateDTO response = new com.KTU.KTUVotingapp.dto.CandidateDTO(
                    saved.getId(), saved.getCategory(), saved.getCandidateNumber(), saved.getName(), saved.getDepartment(), saved.getImageUrl(), saved.getVoteCount()
            );

            return ResponseEntity.ok(response);
        } catch (RankingConflictException rce) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error","RANK_CONFLICT","message", rce.getMessage()));
        } catch (DataIntegrityViolationException dive) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error","CONFLICT","message","Candidate number already in use in this category"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error","CREATE_FAILED","message", e.getMessage()));
        }
    }

    // New: multipart/form-data create endpoint (accepts file optionally)
    @PostMapping(value = "/candidates", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, params = "adminPin")
    public org.springframework.http.ResponseEntity<?> createCandidateMultipart(@RequestParam("adminPin") String pin,
                                                                                 @ModelAttribute CandidateForm form) {
        if (pin == null || !pin.equals(adminPin)) {
            return org.springframework.http.ResponseEntity.status(403).body("Forbidden");
        }

        com.KTU.KTUVotingapp.model.Candidate candidate = new com.KTU.KTUVotingapp.model.Candidate();
        if (form.getCategory() != null) candidate.setCategory(form.getCategory());
        if (form.getCandidateNumber() != null) candidate.setCandidateNumber(form.getCandidateNumber());
        candidate.setName(form.getName());
        candidate.setDepartment(form.getDepartment());
        candidate.setVoteCount(form.getVoteCount() != null ? form.getVoteCount() : 0L);

        try {
            if (form.getImage() != null && !form.getImage().isEmpty()) {
                String url = imageStorageService.store(form.getImage());
                candidate.setImageUrl(url);
            } else if (form.getImageUrl() != null) {
                // If front-end provided an explicit imageUrl string, allow it
                candidate.setImageUrl(form.getImageUrl());
            }

            // Use transactional service which will validate and use SERIALIZABLE isolation
            try {
                validateCandidateNumberOrThrow(candidate.getCandidateNumber());
            } catch (IllegalArgumentException ex) {
                return org.springframework.http.ResponseEntity.badRequest().body(Map.of("error","INVALID_NUMBER","message",ex.getMessage()));
            }
            com.KTU.KTUVotingapp.model.Candidate saved = null;
            try {
                saved = candidateService.createCandidateTransactional(candidate);
            } catch (RankingConflictException rce) {
                return org.springframework.http.ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error","RANK_CONFLICT","message", rce.getMessage()));
            } catch (DataIntegrityViolationException dive) {
                return org.springframework.http.ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error","CONFLICT","message","Candidate number already in use in this category"));
            }

            com.KTU.KTUVotingapp.dto.CandidateDTO response = new com.KTU.KTUVotingapp.dto.CandidateDTO(
                    saved.getId(), saved.getCategory(), saved.getCandidateNumber(), saved.getName(), saved.getDepartment(), saved.getImageUrl(), saved.getVoteCount()
            );

            return org.springframework.http.ResponseEntity.ok(response);
        } catch (Exception e) {
            // RankingConflictException is handled by ControllerAdvice to return 409 conflict
            return org.springframework.http.ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/candidates/{id}")
    public org.springframework.http.ResponseEntity<?> getCandidate(@RequestParam("adminPin") String pin, @PathVariable Long id) {
        if (pin == null || !pin.equals(adminPin)) {
            return org.springframework.http.ResponseEntity.status(403).body("Forbidden");
        }

        java.util.Optional<com.KTU.KTUVotingapp.model.Candidate> found = candidateRepository.findById(id);
        if (found.isEmpty()) return org.springframework.http.ResponseEntity.notFound().build();

        com.KTU.KTUVotingapp.model.Candidate c = found.get();
        com.KTU.KTUVotingapp.dto.CandidateDTO response = new com.KTU.KTUVotingapp.dto.CandidateDTO(
                c.getId(), c.getCategory(), c.getCandidateNumber(), c.getName(), c.getDepartment(), c.getImageUrl(), c.getVoteCount()
        );
        return org.springframework.http.ResponseEntity.ok(response);
    }

    @PutMapping(value = "/candidates/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateCandidate(@RequestParam("adminPin") String pin, @PathVariable Long id,
                                             @RequestBody com.KTU.KTUVotingapp.dto.CandidateDTO dto) {
        if (pin == null || !pin.equals(adminPin)) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        CandidateForm form = new CandidateForm();
        form.setCategory(dto.getCategory());
        form.setCandidateNumber(dto.getCandidateNumber());
        form.setName(dto.getName());
        form.setDepartment(dto.getDepartment());
        form.setVoteCount(dto.getVoteCount());
        form.setImageUrl(dto.getImageUrl());

        try {
            if (dto.getCandidateNumber() != null) validateCandidateNumberOrThrow(dto.getCandidateNumber());
            Candidate saved = candidateService.updateCandidateTransactional(id, form, existing -> {
                if (dto.getImageUrl() != null) {
                    existing.setImageUrl(dto.getImageUrl());
                }
            });
            candidateService.evictCaches();

            com.KTU.KTUVotingapp.dto.CandidateDTO response = new com.KTU.KTUVotingapp.dto.CandidateDTO(
                    saved.getId(), saved.getCategory(), saved.getCandidateNumber(), saved.getName(), saved.getDepartment(), saved.getImageUrl(), saved.getVoteCount()
            );
            return ResponseEntity.ok(response);
        } catch (RankingConflictException rce) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error","RANK_CONFLICT","message", rce.getMessage()));
        } catch (DataIntegrityViolationException dive) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error","CONFLICT","message","Candidate number already in use in this category"));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(Map.of("error","INVALID_NUMBER","message", iae.getMessage()));
        }
    }

    // New: multipart/form-data update endpoint (accepts file optionally)
    @RequestMapping(value = "/candidates/{id}", method = {org.springframework.web.bind.annotation.RequestMethod.PUT, org.springframework.web.bind.annotation.RequestMethod.POST}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, params = "adminPin")
    public org.springframework.http.ResponseEntity<?> updateCandidateMultipart(@RequestParam("adminPin") String pin, @PathVariable Long id,
                                                                                 @ModelAttribute CandidateForm form) {
        if (pin == null || !pin.equals(adminPin)) {
            return org.springframework.http.ResponseEntity.status(403).body("Forbidden");
        }

        try {
            // imageUpdater will store file if provided and set imageUrl on the candidate
            java.util.function.Consumer<com.KTU.KTUVotingapp.model.Candidate> imageUpdater = c -> {
                try {
                    if (form.getImage() != null && !form.getImage().isEmpty()) {
                        String url = imageStorageService.store(form.getImage());
                        c.setImageUrl(url);
                    } else if (form.getImageUrl() != null) {
                        c.setImageUrl(form.getImageUrl());
                    }
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to store image: " + ex.getMessage(), ex);
                }
            };

            try {
                if (form.getCandidateNumber() != null) validateCandidateNumberOrThrow(form.getCandidateNumber());
                com.KTU.KTUVotingapp.model.Candidate saved = candidateService.updateCandidateTransactional(id, form, imageUpdater);

                com.KTU.KTUVotingapp.dto.CandidateDTO response = new com.KTU.KTUVotingapp.dto.CandidateDTO(
                        saved.getId(), saved.getCategory(), saved.getCandidateNumber(), saved.getName(), saved.getDepartment(), saved.getImageUrl(), saved.getVoteCount()
                );

                return org.springframework.http.ResponseEntity.ok(response);
            } catch (RankingConflictException rce) {
                return org.springframework.http.ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error","RANK_CONFLICT","message", rce.getMessage()));
            } catch (DataIntegrityViolationException dive) {
                return org.springframework.http.ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error","CONFLICT","message","Candidate number already in use in this category"));
            } catch (IllegalArgumentException iae) {
                return org.springframework.http.ResponseEntity.badRequest().body(Map.of("error","INVALID_NUMBER","message", iae.getMessage()));
            }
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/candidates/{id}")
    public ResponseEntity<?> deleteCandidate(@RequestParam("adminPin") String pin, @PathVariable Long id) {
        if (pin == null || !pin.equals(adminPin) ){
            return ResponseEntity.status(403).body("Forbidden");
        }

        if (!candidateRepository.existsById(id)) return ResponseEntity.notFound().build();
        candidateService.deleteCandidate(id);
        candidateService.evictCaches();
        return ResponseEntity.noContent().build();
    }

    // New: rank check endpoint used by frontend to validate before submitting
    @GetMapping("/candidates/check-rank")
    public ResponseEntity<?> checkRank(@RequestParam("category") String categoryStr,
                                       @RequestParam("number") Integer number,
                                       @RequestParam(value = "excludeId", required = false) Long excludeId) {
        try {
            Category category = Category.valueOf(categoryStr.toUpperCase());
            boolean taken = candidateService.isRankTakenInPaired(category, number, excludeId);
            return ResponseEntity.ok(Map.of("taken", taken));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "INVALID_CATEGORY"));
        }
    }

    @PostMapping("/candidates/reset-votes")
    public ResponseEntity<?> resetAllVotes(@RequestParam("adminPin") String pin,
                                           @RequestParam(value = "category", required = false) String categoryStr,
                                           @RequestParam(value = "performedBy", required = false) String performedBy) {
        if (pin == null || !pin.equals(adminPin)) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        try {
            if (categoryStr != null && !categoryStr.isBlank()) {
                try {
                    Category category = Category.valueOf(categoryStr.toUpperCase());
                    int updated = candidateService.resetVotesByCategory(category, performedBy);
                    return ResponseEntity.ok(Map.of("updatedRows", updated, "message", "Candidate vote counts reset for category " + category));
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(Map.of("error", "INVALID_CATEGORY", "message", "Category not recognized: " + categoryStr));
                }
            } else {
                int updated = candidateService.resetAllVotes(performedBy);
                return ResponseEntity.ok(Map.of("updatedRows", updated, "message", "All candidate vote counts have been reset to 0."));
            }
         } catch (Exception e) {
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "RESET_FAILED", "message", e.getMessage()));
         }
     }

    /**
     * Returns recent admin audit records (most recent first). Protected by adminPin.
     */
    @GetMapping("/audit")
    public ResponseEntity<?> getAudit(@RequestParam("adminPin") String pin,
                                      @RequestParam(value = "limit", required = false, defaultValue = "50") int limit) {
        if (pin == null || !pin.equals(adminPin)) {
            return ResponseEntity.status(403).build();
        }

        int pageSize = Math.max(1, Math.min(limit, 200));
        PageRequest pr = PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "performedAt"));
        var page = auditRepository.findAll(pr);
        return ResponseEntity.ok(page.getContent());
    }
}
