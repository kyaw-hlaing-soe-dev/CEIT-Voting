package com.KTU.KTUVotingapp.service;

import com.KTU.KTUVotingapp.model.VoterPin;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

@Service
public class AdminService {

    private final PinService pinService;

    public AdminService(PinService pinService) {
        this.pinService = pinService;
    }

    /**
     * Generate exactly `count` unique 7-digit pins and persist them.
     */
    public List<String> generatePins(int count) {
        return pinService.generatePins(count);
    }

    /**
     * Export all generated pins as CSV with headers: PIN_CODE, STATUS, USED_AT
     */
    public String exportPinsCsv() throws IOException {
        List<VoterPin> all = pinService.listAllPins();
        StringWriter out = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader("PIN_CODE", "STATUS", "USED_AT"))) {
            for (VoterPin vp : all) {
                String status = vp.isUsed() ? "Used" : "Active";
                printer.printRecord(vp.getPinCode(), status, vp.getUsedAt());
            }
        }
        return out.toString();
    }
}
