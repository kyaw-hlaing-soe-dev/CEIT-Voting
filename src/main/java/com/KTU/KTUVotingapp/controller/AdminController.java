package com.KTU.KTUVotingapp.controller;

import com.KTU.KTUVotingapp.dto.ResultDTO;
import com.KTU.KTUVotingapp.exception.RankingConflictException;
import com.KTU.KTUVotingapp.model.Candidate;
import com.KTU.KTUVotingapp.model.Category;
import com.KTU.KTUVotingapp.service.ResultService;
import com.KTU.KTUVotingapp.repository.CandidateRepository;
import com.KTU.KTUVotingapp.service.ImageStorageService;
import com.KTU.KTUVotingapp.dto.CandidateForm;
import com.KTU.KTUVotingapp.service.CandidateService;
import com.KTU.KTUVotingapp.repository.AdminActionAuditRepository;
import com.KTU.KTUVotingapp.service.PinService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final ResultService resultService;
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    // adminPin will be injected from application properties (voting.admin-pin)
    private final String adminPin;

    // Inject repository directly to avoid costly/contextual lookups per request
    private final CandidateRepository candidateRepository;

    private final ImageStorageService imageStorageService;

    private final CandidateService candidateService;

    private final AdminActionAuditRepository auditRepository;

    private final PinService pinService;

    private final com.KTU.KTUVotingapp.service.AdminService adminService;

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

    public AdminController(ResultService resultService, CandidateRepository candidateRepository, ImageStorageService imageStorageService, CandidateService candidateService, AdminActionAuditRepository auditRepository, PinService pinService, com.KTU.KTUVotingapp.service.AdminService adminService, @Value("${voting.admin-pin:}") String adminPin) {
        this.resultService = resultService;
        this.adminPin = (adminPin == null ? "" : adminPin.trim());
        this.candidateRepository = candidateRepository;
        this.imageStorageService = imageStorageService;
        this.candidateService = candidateService;
        this.auditRepository = auditRepository;
        this.pinService = pinService;
        this.adminService = adminService;

        // Do not throw during startup if adminPin missing; log a warning so the admin dashboard can show a proper message.
        if (this.adminPin.isBlank()) {
            log.warn("voting.admin-pin not configured; admin endpoints require either an adminPin query parameter or a validated admin session.");
        } else if (!this.adminPin.matches("^\\d{7}$")) {
            log.warn("voting.admin-pin value '{}' does not match expected 7-digit pattern; admin endpoints will still require this exact value or a validated session.", this.adminPin);
        }
    }

    /**
     * Live admin stats and flattened candidate results for dashboard polling.
     * Accepts either an adminPin query parameter OR an authenticated admin session
     * created via /api/admin/validate. This avoids a 400 response when adminPin
     * is omitted and supports both session-based and one-shot pin auth.
     */
    @GetMapping("/results")
    public ResponseEntity<?> getLiveAdminResults(@RequestParam(value = "adminPin", required = false) String adminPinParam,
                                                 @RequestParam(value = "pin", required = false) String legacyPinParam,
                                                 HttpSession session) {
        // Prefer the explicit adminPin param, but accept legacy "pin" param for backward compatibility
        String pin = adminPinParam != null ? adminPinParam : legacyPinParam;

        boolean authorized = false;
        if (pin != null && pin.equals(adminPin)) authorized = true;
        if (!authorized && isAdminSessionAuthenticated(session)) authorized = true;
        if (!authorized) return ResponseEntity.status(403).body("Forbidden");

        // Build stats payload
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
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "RANK_CONFLICT", "message", rce.getMessage()));
        } catch (DataIntegrityViolationException dive) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "CONFLICT", "message", "Candidate number already in use in this category"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "CREATE_FAILED", "message", e.getMessage()));
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
                return org.springframework.http.ResponseEntity.badRequest().body(Map.of("error", "INVALID_NUMBER", "message", ex.getMessage()));
            }
            com.KTU.KTUVotingapp.model.Candidate saved = null;
            try {
                saved = candidateService.createCandidateTransactional(candidate);
            } catch (RankingConflictException rce) {
                return org.springframework.http.ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "RANK_CONFLICT", "message", rce.getMessage()));
            } catch (DataIntegrityViolationException dive) {
                return org.springframework.http.ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "CONFLICT", "message", "Candidate number already in use in this category"));
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
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "RANK_CONFLICT", "message", rce.getMessage()));
        } catch (DataIntegrityViolationException dive) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "CONFLICT", "message", "Candidate number already in use in this category"));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(Map.of("error", "INVALID_NUMBER", "message", iae.getMessage()));
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
                return org.springframework.http.ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "RANK_CONFLICT", "message", rce.getMessage()));
            } catch (DataIntegrityViolationException dive) {
                return org.springframework.http.ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "CONFLICT", "message", "Candidate number already in use in this category"));
            } catch (IllegalArgumentException iae) {
                return org.springframework.http.ResponseEntity.badRequest().body(Map.of("error", "INVALID_NUMBER", "message", iae.getMessage()));
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
                    java.util.Map<String, Object> result = candidateService.resetVotesByCategoryAndHistory(category, performedBy);
                    return ResponseEntity.ok(Map.of("result", result, "message", "Candidate vote counts and history reset for category " + category));
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(Map.of("error", "INVALID_CATEGORY", "message", "Category not recognized: " + categoryStr));
                }
            } else {
                java.util.Map<String, Object> result = candidateService.resetAllVotesAndHistory(performedBy);
                return ResponseEntity.ok(Map.of("result", result, "message", "All candidate vote counts and vote history have been reset."));
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

    // PIN management endpoints (placeholders)
    @PostMapping("/pin/generate")
    public ResponseEntity<?> generatePins(@RequestParam("adminPin") String pin,
                                          @RequestParam("count") int count) {
        if (!adminPin.equals(pin)) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden"));
        }
        if (count <= 0 || count > 20000) {
            return ResponseEntity.badRequest().body(Map.of("message", "Count must be between 1 and 20000"));
        }
        var pins = adminService.generatePins(count);
        return ResponseEntity.ok(Map.of("generated", pins.size(), "pins", pins));
    }

    @GetMapping("/pin")
    public ResponseEntity<?> listPins(@RequestParam("adminPin") String pin,
                                      @RequestParam(value = "page", defaultValue = "0") int page,
                                      @RequestParam(value = "size", defaultValue = "50") int size) {
        if (!adminPin.equals(pin)) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden"));
        }
        Page<com.KTU.KTUVotingapp.model.VoterPin> result = pinService.listPins(page, Math.min(size, 500));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/pins")
    public ResponseEntity<?> listPinsPaginated(@RequestParam("adminPin") String pin,
                                               @RequestParam(value = "page", defaultValue = "0") int page,
                                               @RequestParam(value = "size", defaultValue = "50") int size,
                                               @RequestParam(value = "search", required = false) String search) {
        if (!adminPin.equals(pin)) return ResponseEntity.status(403).body(Map.of("message", "Forbidden"));
        var paged = pinService.listPins(page, Math.min(size, 1000));
        // If search provided, filter client-side by pinCode (fast enough for moderate sizes) — for large data implement repository search
        if (search != null && !search.isBlank()) {
            var filtered = paged.getContent().stream().filter(p -> p.getPinCode().contains(search)).toList();
            Map<String, Object> payload = Map.of("total", filtered.size(), "pins", filtered);
            return ResponseEntity.ok(payload);
        }
        return ResponseEntity.ok(paged);
    }

    @GetMapping(value = "/pin/export", produces = "text/csv")
    public ResponseEntity<String> exportPins(@RequestParam("adminPin") String pin) throws java.io.IOException {
        if (!adminPin.equals(pin)) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        String csv = adminService.exportPinsCsv();
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=pin-export.csv")
                .contentType(org.springframework.http.MediaType.valueOf("text/csv"))
                .body(csv);
    }

    @GetMapping(value = "/pins/export", produces = "text/csv")
    public ResponseEntity<org.springframework.core.io.InputStreamResource> exportPinsCsv(@RequestParam("adminPin") String pin) throws java.io.IOException {
        if (!adminPin.equals(pin)) return ResponseEntity.status(403).build();
        String csv = adminService.exportPinsCsv();
        byte[] bytes = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(bytes);
        org.springframework.core.io.InputStreamResource resource = new org.springframework.core.io.InputStreamResource(bais);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=pin-export.csv")
                .contentLength(bytes.length)
                .contentType(org.springframework.http.MediaType.parseMediaType("text/csv; charset=utf-8"))
                .body(resource);
    }

    // New endpoints for pin management
    @PostMapping("/pins/generate")
    public ResponseEntity<?> generatePinsBulk(@RequestParam("adminPin") String pin,
                                              @RequestParam("count") int count) {
        if (!adminPin.equals(pin)) return ResponseEntity.status(403).body(Map.of("message", "Forbidden"));
        if (count <= 0 || count > 100000) return ResponseEntity.badRequest().body(Map.of("message", "Count must be between 1 and 100000"));
        List<String> generated = adminService.generatePins(count);
        return ResponseEntity.ok(Map.of("generatedCount", generated.size()));
    }

    @GetMapping("/pins/search")
    public ResponseEntity<?> searchPins(@RequestParam("adminPin") String pin,
                                        @RequestParam("query") String query) {
        if (!adminPin.equals(pin)) return ResponseEntity.status(403).body(Map.of("message", "Forbidden"));
        if (query == null || query.isBlank()) return ResponseEntity.badRequest().body(Map.of("message", "Query required"));
        // naive search: iterate all pins and filter by contains; for large datasets implement repository method
        List<com.KTU.KTUVotingapp.model.VoterPin> all = pinService.listAllPins();
        var filtered = all.stream().filter(p -> p.getPinCode().contains(query)).map(p -> Map.of("pin", p.getPinCode(), "status", p.isUsed() ? "Used" : "Active", "usedAt", p.getUsedAt())).toList();
        return ResponseEntity.ok(Map.of("pins", filtered));
    }

    @GetMapping(value = "/pins", params = {"adminPin", "offset", "limit"})
    public ResponseEntity<?> listPinsOffset(@RequestParam("adminPin") String pin,
                                            @RequestParam("offset") int offset,
                                            @RequestParam("limit") int limit) {
        if (!adminPin.equals(pin)) return ResponseEntity.status(403).body(Map.of("message", "Forbidden"));
        if (offset < 0 || limit <= 0) return ResponseEntity.badRequest().body(Map.of("message", "Invalid offset/limit"));
        List<com.KTU.KTUVotingapp.model.VoterPin> all = pinService.listAllPins();
        int from = Math.min(offset, all.size());
        int to = Math.min(offset + limit, all.size());
        var sub = all.subList(from, to).stream().map(p -> Map.of("pin", p.getPinCode(), "status", p.isUsed() ? "Used" : "Active", "usedAt", p.getUsedAt())).toList();
        return ResponseEntity.ok(Map.of("pins", sub));
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateAdmin(@RequestParam("adminPin") String pin, HttpSession session) {
        if (pin == null || !pin.equals(adminPin)) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        // mark session authenticated for subsequent admin requests
        if (session != null) {
            session.setAttribute("adminAuthenticated", Boolean.TRUE);
        }
        return ResponseEntity.ok(Map.of("authenticated", true));
    }

    // helper used by some endpoints isAdminAuthenticated (optional)
    private boolean isAdminSessionAuthenticated(HttpSession session) {
        return session != null && Boolean.TRUE.equals(session.getAttribute("adminAuthenticated"));
    }

    @GetMapping("/session")
    public ResponseEntity<?> checkAdminSession(HttpSession session) {
        boolean auth = Boolean.TRUE.equals(session.getAttribute("adminAuthenticated"));
        return ResponseEntity.ok(Map.of("authenticated", auth));
    }

    // Debug endpoint to inspect whether an adminPin is configured (does NOT reveal the pin value). Safe for dev.
    @GetMapping("/debug/config")
    public ResponseEntity<?> debugConfig(HttpServletRequest request) {
        // Return non-sensitive debug information about whether an adminPin is configured.
        Map<String, Object> info = new HashMap<>();
        info.put("adminPinConfigured", !this.adminPin.isBlank());
        info.put("adminPinLength", this.adminPin.length());
        return ResponseEntity.ok(info);
    }
}
