package com.finsaathi.SipCalculator.controller;

import com.finsaathi.SipCalculator.model.GoalCalculation;
import com.finsaathi.SipCalculator.model.SchemeSipSuggestion;
import com.finsaathi.SipCalculator.model.User;
import com.finsaathi.SipCalculator.model.UserRequest;
import com.finsaathi.SipCalculator.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.Map;

@RestController
@RequestMapping("/api/reports") // Base path for report-related endpoints
public class ReportController {

    private static final Logger logger = Logger.getLogger(ReportController.class.getName());

    @Autowired
    private UserService userService;
    @Autowired
    private UserRequestService userRequestService;
    @Autowired
    private InvestmentSchemeService investmentSchemeService;
    @Autowired
    private PdfGeneratorService pdfGeneratorService;
    @Autowired
    private EmailService emailService;

    /**
     * Generates a PDF report for a specific user request (transaction) and returns it as a download.
     * Maps to GET /api/reports/pdf/{requestId}
     * @param requestId The ID of the user request (Transaction ID) for which to generate the report.
     * @return ResponseEntity with the PDF bytes for download or an error status.
     */
    @GetMapping(value = "/pdf/{requestId}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getPdfReport(@PathVariable UUID requestId) {
        logger.info("Received request to generate PDF for request ID: " + requestId);

        Optional<UserRequest> userRequestOptional = userRequestService.getUserRequestById(requestId);
        if (userRequestOptional.isEmpty()) {
            logger.warning("PDF report requested for non-existent request ID: " + requestId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        UserRequest userRequest = userRequestOptional.get();

        Optional<GoalCalculation> goalCalculationOptional = userRequestService.getGoalCalculationByRequestId(requestId);
        if (goalCalculationOptional.isEmpty()) {
            logger.severe("No calculation found for user request ID: " + requestId + ". Cannot generate PDF.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        GoalCalculation goalCalculation = goalCalculationOptional.get();

        Optional<User> userOptional = userService.getUserById(userRequest.getUserId());
        if (userOptional.isEmpty()) {
            logger.severe("User not found for user request " + requestId + " (userId: " + userRequest.getUserId() + "). This is an unexpected state.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        User user = userOptional.get();

        // Determine the overall optimal monthly SIP needed for the allocation breakdown in the PDF
        BigDecimal overallOptimalMonthlySip;
        if (goalCalculation.getMonthlySipRequiredBestWeightedCase() != null) {
            overallOptimalMonthlySip = goalCalculation.getMonthlySipRequiredBestWeightedCase();
        } else if (userRequest.getFutureValue() != null) {
            overallOptimalMonthlySip = userRequest.getFutureValue();
        } else {
            logger.warning("GoalCalculation found for request ID " + requestId + " but no valid overall optimal monthly SIP could be determined for PDF allocation.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        // Fetch the 50-30-20 allocated scheme suggestions for the PDF
        List<SchemeSipSuggestion> allocatedSuggestions = investmentSchemeService.getOptimalAllocatedSchemeSuggestions(
                overallOptimalMonthlySip,
                userRequest.getTimePeriodYears()
        );

        try {
            // Generate the PDF
            byte[] pdfBytes = pdfGeneratorService.generatePdfReport(user, userRequest, goalCalculation, allocatedSuggestions);

            // Set HTTP Headers for PDF download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = "SIP_Report_" + user.getName().replaceAll("\\s+", "_") + "_Request_" + requestId.toString().substring(0, 8) + ".pdf";
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            logger.info("Generated PDF report successfully for request ID: " + requestId);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (IOException e) {
            logger.severe("Error generating PDF report for request ID " + requestId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Generates a PDF report and emails it to the user associated with the request.
     * Maps to POST /api/reports/email/{requestId}
     * @param requestId The ID of the user request (Transaction ID).
     * @param payload A map that MUST contain an "email" field.
     * @return ResponseEntity with success/failure message.
     */
    @PostMapping("/email/{requestId}")
    public ResponseEntity<Map<String, String>> emailPdfReport(@PathVariable UUID requestId,
                                                              @RequestBody Map<String, String> payload) {
        logger.info("Received request to email PDF for request ID: " + requestId);

        // Validate email from payload immediately
        String recipientEmail = payload.get("email");
        if (recipientEmail == null || recipientEmail.isBlank() || !recipientEmail.contains("@") || !recipientEmail.contains(".")) {
            logger.warning("Invalid email address provided in request body for request ID: " + requestId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "A valid email address must be provided in the request body."));
        }
        logger.info("Recipient email from request body: " + recipientEmail);


        Optional<UserRequest> userRequestOptional = userRequestService.getUserRequestById(requestId);
        if (userRequestOptional.isEmpty()) {
            logger.warning("Email report requested for non-existent request ID: " + requestId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Request not found."));
        }
        UserRequest userRequest = userRequestOptional.get();

        Optional<GoalCalculation> goalCalculationOptional = userRequestService.getGoalCalculationByRequestId(requestId);
        if (goalCalculationOptional.isEmpty()) {
            logger.severe("No calculation found for user request ID: " + requestId + ". Cannot email PDF.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Calculation results not found for this request."));
        }
        GoalCalculation goalCalculation = goalCalculationOptional.get();

        Optional<User> userOptional = userService.getUserById(userRequest.getUserId());
        if (userOptional.isEmpty()) {
            logger.severe("User not found for user request " + requestId + " (userId: " + userRequest.getUserId() + "). Cannot email PDF.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found for this request."));
        }
        User user = userOptional.get();

        // --- NEW LOGIC: Save the provided email to the user's profile ---
        if (!recipientEmail.equals(user.getEmail())) { // Only update if different
            userService.updateUserEmail(user.getId(), recipientEmail);
            logger.info("Updated user " + user.getId() + " email to: " + recipientEmail);
        }
        // --- END NEW LOGIC ---

        // Determine the overall optimal monthly SIP needed for the allocation breakdown in the PDF email
        BigDecimal overallOptimalMonthlySip;
        if (goalCalculation.getMonthlySipRequiredBestWeightedCase() != null) {
            overallOptimalMonthlySip = goalCalculation.getMonthlySipRequiredBestWeightedCase();
        } else if (userRequest.getFutureValue() != null) {
            overallOptimalMonthlySip = userRequest.getFutureValue();
        } else {
            logger.warning("GoalCalculation found for request ID " + requestId + " but no valid overall optimal monthly SIP could be determined for PDF email allocation.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Could not determine target monthly SIP for email allocation."));
        }

        List<SchemeSipSuggestion> allocatedSuggestions = investmentSchemeService.getOptimalAllocatedSchemeSuggestions(
                overallOptimalMonthlySip,
                userRequest.getTimePeriodYears()
        );

        try {
            // Generate PDF bytes
            byte[] pdfBytes = pdfGeneratorService.generatePdfReport(user, userRequest, goalCalculation, allocatedSuggestions);
            String filename = "SIP_Report_" + user.getName().replaceAll("\\s+", "_") + "_Request_" + requestId.toString().substring(0, 8) + ".pdf";
            String subject = "Your SIP Goal Tracking Report - Request ID: " + requestId.toString().substring(0, 8);
            String body = "Dear " + user.getName() + ",<br><br>" +
                    "Please find attached your SIP Goal Tracking Report for your dream of " + userRequest.getDreamType() + "." +
                    "<br><br>Thank you for using our service!";

            // Send the email with the PDF attachment
            emailService.sendEmailWithAttachment(recipientEmail, subject, body, pdfBytes, filename);

            logger.info("PDF report emailed successfully for request ID: " + requestId + " to " + recipientEmail);
            return ResponseEntity.ok(Map.of("message", "PDF report emailed successfully to " + recipientEmail + "."));
        } catch (IOException e) {
            logger.severe("Error generating or emailing PDF report for request ID " + requestId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Failed to generate or email PDF report."));
        }
    }
}






