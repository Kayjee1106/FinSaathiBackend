package com.finsaathi.SipCalculator.controller;

import com.finsaathi.SipCalculator.model.InvestmentSchemeRate;
import com.finsaathi.SipCalculator.service.InvestmentSchemeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/schemes")
public class InvestmentSchemeController {
    private static final Logger logger = Logger.getLogger(InvestmentSchemeController.class.getName());

    @Autowired
    private InvestmentSchemeService investmentSchemeService;

    @GetMapping
    public ResponseEntity<List<InvestmentSchemeRate>> getAllSchemes() {
        logger.info("Received request to get all investment schemes.");
        List<InvestmentSchemeRate> schemes = investmentSchemeService.getAllSchemes();
        return ResponseEntity.ok(schemes); // Returns 200 OK with the list of schemes
    }

    @GetMapping("/year/{year}")
    public ResponseEntity<List<InvestmentSchemeRate>> getSchemesByYear(@PathVariable Integer year) {
        logger.info("Received request to get investment schemes for year: " + year);
        if (year == null || (year < 1 || year > 5 || (year !=1 && year !=2 && year !=3 && year !=4 && year !=5))) {
            logger.warning("Invalid year provided: " + year);
            return ResponseEntity.badRequest().body(null);
        }
        List<InvestmentSchemeRate> schemes = investmentSchemeService.getSchemeRatesByYear(year);
        return ResponseEntity.ok(schemes);
    }

}
