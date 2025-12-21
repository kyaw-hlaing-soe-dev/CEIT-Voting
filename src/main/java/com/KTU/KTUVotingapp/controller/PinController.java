package com.KTU.KTUVotingapp.controller;

import com.KTU.KTUVotingapp.model.VoterPin;
import com.KTU.KTUVotingapp.service.PinService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pins")
@CrossOrigin(origins = "*")
public class PinController {

    private final PinService pinService;

    @Value("${voting.admin-pin}")
    private String adminPin;

    public PinController(PinService pinService) {
        this.pinService = pinService;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generatePins(@RequestParam("adminPin") String pin,
                                          @RequestParam("count") int count) {
        if (!adminPin.equals(pin)) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden"));
        }
        if (count <= 0 || count > 20000) {
            return ResponseEntity.badRequest().body(Map.of("message", "Count must be between 1 and 20000"));
        }
        List<String> pins = pinService.generatePins(count);
        return ResponseEntity.ok(Map.of("generated", pins.size(), "pins", pins));
    }

    @GetMapping
    public ResponseEntity<?> listPins(@RequestParam("adminPin") String pin,
                                      @RequestParam(value = "page", defaultValue = "0") int page,
                                      @RequestParam(value = "size", defaultValue = "50") int size) {
        if (!adminPin.equals(pin)) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden"));
        }
        Page<VoterPin> result = pinService.listPins(page, Math.min(size, 500));
        return ResponseEntity.ok(result);
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<String> exportPins(@RequestParam("adminPin") String pin) throws IOException {
        if (!adminPin.equals(pin)) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        List<VoterPin> allPins = pinService.listAllPins();
        StringWriter out = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader("PIN_CODE", "STATUS", "USED_AT"))) {
            for (VoterPin vp : allPins) {
                String status = vp.isUsed() ? "Used" : "Active";
                printer.printRecord(vp.getPinCode(), status, vp.getUsedAt());
            }
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=pin-export.csv")
                .contentType(MediaType.valueOf("text/csv"))
                .body(out.toString());
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPin(@RequestBody Map<String, String> body) {
        String pin = body.getOrDefault("pin", "");
        if (pin.length() != 7) {
            return ResponseEntity.badRequest().body(Map.of("message", "PIN must be 7 digits"));
        }
        try {
            // Token valid for 10 minutes
            String token = pinService.issueSessionToken(pin, LocalDateTime.now().plus(10, ChronoUnit.MINUTES));
            return ResponseEntity.ok(Map.of("token", token));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("message", "Invalid PIN"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("message", e.getMessage()));
        }
    }
}
