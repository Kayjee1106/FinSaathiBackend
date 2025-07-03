package com.finsaathi.SipCalculator.controller;

import com.finsaathi.SipCalculator.model.GoalCalculation;
import com.finsaathi.SipCalculator.model.SchemeSipSuggestion;
import com.finsaathi.SipCalculator.model.UserRequest;
import com.finsaathi.SipCalculator.service.InvestmentSchemeService;
import com.finsaathi.SipCalculator.service.UserRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/requests")
public class UserRequestController {

    private static final Logger logger = Logger.getLogger(UserRequestController.class.getName());

    @Autowired
    private UserRequestService userRequestService;
    @Autowired
    private InvestmentSchemeService investmentSchemeService;

    @PostMapping("/calculate-and-save")
    public ResponseEntity<?> createAndSaveUserRequest(
            @RequestParam UUID userId,
            @RequestParam BigDecimal futureValue,
            @RequestParam Integer timePeriodYears,
            @RequestParam String dreamType) {

        logger.info("Received request to create and save user request (SIP from FV) for user: " + userId);
        if (futureValue == null || futureValue.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Future value must be a positive number."));
        }
        if (timePeriodYears == null || (timePeriodYears < 1 || timePeriodYears > 5 || (timePeriodYears !=1 && timePeriodYears !=2 && timePeriodYears !=3 && timePeriodYears !=4 && timePeriodYears !=5))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid time period. Time period must be 1, 2, 3, 4, or 5 years."));
        }
        if (dreamType == null || dreamType.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Dream type cannot be empty."));
        }

        try {
            UserRequest savedUserRequest = userRequestService.createNewUserRequestAndCalculateSip(userId, futureValue, timePeriodYears, dreamType);
            Optional<GoalCalculation> goalCalculation = userRequestService.getGoalCalculationByRequestId(savedUserRequest.getId());

            if (goalCalculation.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("requestDetails", savedUserRequest);
                response.put("calculationResults", goalCalculation.get());
                logger.info("New user request and calculation saved. Request ID: " + savedUserRequest.getId());
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                logger.severe("Calculation not found for newly created request: " + savedUserRequest.getId() + ". This indicates a potential data saving issue.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Calculation results could not be retrieved for the new request."));
            }

        } catch (Exception e) {
            logger.severe("Error creating/saving new user request: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to create or save user request", "details", e.getMessage()));
        }
    }

    /** What-if flow: Calculates Future Value from a user-provided Monthly SIP amount. */
    @PostMapping("/calculate-fv-from-sip")
    public ResponseEntity<?> calculateFvFromUserSip(
            @RequestParam UUID userId,
            @RequestParam BigDecimal monthlySipAmount,
            @RequestParam Integer timePeriodYears,
            @RequestParam(required = false) String dreamType) {

        logger.info("Received request to calculate FV from SIP for user: " + userId + ", SIP: " + monthlySipAmount + ", Years: " + timePeriodYears);
        if (monthlySipAmount == null || monthlySipAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Monthly SIP amount must be a positive number."));
        }
        if (timePeriodYears == null || (timePeriodYears < 1 || timePeriodYears > 5 || (timePeriodYears !=1 && timePeriodYears !=2 && timePeriodYears !=3 && timePeriodYears !=4 && timePeriodYears !=5))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid time period. Time period must be 1, 2, 3, 4, or 5 years."));
        }

        try {
            UserRequest savedUserRequest = userRequestService.createFutureValueCalculationForUserSip(userId, monthlySipAmount, timePeriodYears, dreamType);
            Optional<GoalCalculation> goalCalculation = userRequestService.getGoalCalculationByRequestId(savedUserRequest.getId());

            if (goalCalculation.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("requestDetails", savedUserRequest);
                response.put("calculationResults", goalCalculation.get());
                logger.info("New FV from SIP request and calculation saved. Request ID: " + savedUserRequest.getId());
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                logger.severe("Calculation not found for newly created FV from SIP request: " + savedUserRequest.getId() + ". This indicates a potential data saving issue.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Calculation results could not be retrieved for the new request."));
            }

        } catch (Exception e) {
            logger.severe("Error creating/saving FV from SIP request: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to create or save FV from SIP request", "details", e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserRequest>> getUserRequestsByUserId(@PathVariable UUID userId) {
        logger.info("Received request to get all user requests for user ID: " + userId);
        List<UserRequest> userRequests = userRequestService.getUserRequestsByUserId(userId);
        return ResponseEntity.ok(userRequests);
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<?> getUserRequestDetails(@PathVariable UUID requestId) {
        logger.info("Received request to get details for request ID: " + requestId);
        Optional<UserRequest> userRequestOptional = userRequestService.getUserRequestById(requestId);
        if (userRequestOptional.isEmpty()) {
            logger.warning("User request not found for ID: " + requestId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Request not found."));
        }

        Optional<GoalCalculation> goalCalculationOptional = userRequestService.getGoalCalculationByRequestId(requestId);
        if (goalCalculationOptional.isEmpty()) {
            logger.warning("User request found, but no associated calculation for request ID: " + requestId + ". This indicates a potential data inconsistency.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Calculation results not found for this request."));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("requestDetails", userRequestOptional.get());
        response.put("calculationResults", goalCalculationOptional.get());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{requestId}/scheme-suggestions")
    public ResponseEntity<?> getSchemeSuggestionsForRequest(@PathVariable UUID requestId) {
        logger.info("Received request to get scheme suggestions for request ID: " + requestId);

        Optional<UserRequest> userRequestOptional = userRequestService.getUserRequestById(requestId);
        if (userRequestOptional.isEmpty()) {
            logger.warning("Scheme suggestions requested for non-existent request ID: " + requestId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Request not found."));
        }
        UserRequest userRequest = userRequestOptional.get();

        Optional<GoalCalculation> goalCalculationOptional = userRequestService.getGoalCalculationByRequestId(requestId);
        if (goalCalculationOptional.isEmpty()) {
            logger.warning("No calculation found for request ID: " + requestId + ". Cannot provide scheme suggestions.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Calculation results not found for this request."));
        }
        GoalCalculation goalCalculation = goalCalculationOptional.get();

        // Determine the overall optimal monthly SIP needed for the allocation breakdown
        BigDecimal overallOptimalMonthlySip;
        if (goalCalculation.getMonthlySipRequiredBestWeightedCase() != null) {
            overallOptimalMonthlySip = goalCalculation.getMonthlySipRequiredBestWeightedCase();
        } else {
            // If the request was a "what-if" (FV from SIP), there's no "monthly_sip_required_best_weighted_case"
            // We need to use the monthly SIP that the user provided in their request.
            overallOptimalMonthlySip = userRequest.getFutureValue(); // userRequest.future_value stores the monthly SIP for this type of request
        }

        List<SchemeSipSuggestion> suggestions = investmentSchemeService.getOptimalAllocatedSchemeSuggestions(
                overallOptimalMonthlySip,
                userRequest.getTimePeriodYears()
        );

        return ResponseEntity.ok(suggestions);
    }
}
