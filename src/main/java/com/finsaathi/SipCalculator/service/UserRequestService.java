package com.finsaathi.SipCalculator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsaathi.SipCalculator.model.GoalCalculation;
import com.finsaathi.SipCalculator.model.LambdaSipResponse;
import com.finsaathi.SipCalculator.model.UserRequest;
import com.finsaathi.SipCalculator.repository.GoalCalculationRepository;
import com.finsaathi.SipCalculator.repository.UserRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@Service // Marks this class as a Spring Service component
public class UserRequestService {

    private static final Logger logger = Logger.getLogger(UserRequestService.class.getName());

    @Autowired // Automatically injects an instance of UserRequestRepository
    private UserRequestRepository userRequestRepository;

    @Autowired // Automatically injects an instance of GoalCalculationRepository
    private GoalCalculationRepository goalCalculationRepository;

    @Autowired // Automatically injects an instance of SipCalculationService (for calling Lambda)
    private SipCalculationService sipCalculationService;

    @Autowired // Automatically injects an instance of ObjectMapper (for JSON serialization/deserialization)
    private ObjectMapper objectMapper;

    /**
     * Creates and saves a new user request for a goal calculation (SIP from FV).
     * This is for the primary flow where user provides a Future Value goal and wants to find the Monthly SIP.
     * It logs the user's request, performs SIP calculation (using existing requests as cache or calling Lambda),
     * and saves the calculation results.
     *
     * @param userId The ID of the user creating the request.
     * @param futureValue The target future value (goal amount).
     * @param timePeriodYears The investment time period in years.
     * @param dreamType The type of dream/goal (e.g., "Retirement", "Property").
     * @return The newly created UserRequest object (representing the transaction).
     */
    @Transactional // Ensures atomicity: all database operations within this method succeed or fail together
    public UserRequest createNewUserRequestAndCalculateSip(UUID userId, BigDecimal futureValue, Integer timePeriodYears, String dreamType) {
        // 1. Create and save the new UserRequest (Table 1 - logs the user's input/transaction)
        UserRequest userRequest = new UserRequest();
        userRequest.setUserId(userId);
        userRequest.setFutureValue(futureValue); // For this operation, future_value is the TARGET
        userRequest.setTimePeriodYears(timePeriodYears);
        userRequest.setDreamType(dreamType);
        // Saving here generates the unique 'id' (Transaction ID) for this request
        userRequest = userRequestRepository.save(userRequest);

        // 2. Retrieve or calculate SIP details using our caching strategy
        // Operation: "calculate_sip_from_fv"
        // Parameters: futureValue is provided, monthlySipAmount is null
        LambdaSipResponse sipResponse = getOrCreateSipCalculationFromUserRequests(
                "calculate_sip_from_fv", futureValue, timePeriodYears, null);

        // 3. Create and save the GoalCalculation (Table 2 - stores the actual calculated results)
        GoalCalculation goalCalculation = new GoalCalculation();
        goalCalculation.setRequestId(userRequest.getId()); // Link this calculation to the new UserRequest

        // Populate GoalCalculation fields from the Lambda response
        populateGoalCalculationWithSipResponse(goalCalculation, sipResponse);

        goalCalculationRepository.save(goalCalculation); // Save the calculation results

        return userRequest; // Return the UserRequest as it represents the "transaction"
    }

    /**
     * Creates and saves a new user request for a Future Value calculation from a custom Monthly SIP.
     * This is for the "what-if" scenario where the user provides a Monthly SIP amount and wants to see the projected Future Value.
     * A new UserRequest is logged for this calculation.
     *
     * @param userId The ID of the user creating the request.
     * @param monthlySipAmount The monthly SIP amount provided by the user.
     * @param timePeriodYears The investment time period in years.
     * @param dreamType An optional dream type for this what-if scenario (defaults to "Custom SIP What-If").
     * @return The newly created UserRequest object (representing the what-if transaction).
     */
    @Transactional
    public UserRequest createFutureValueCalculationForUserSip(UUID userId, BigDecimal monthlySipAmount, Integer timePeriodYears, String dreamType) {
        UserRequest userRequest = new UserRequest();
        userRequest.setUserId(userId);
        userRequest.setFutureValue(monthlySipAmount); // For this operation type, store the monthly SIP in the 'future_value' column of UserRequest
        userRequest.setTimePeriodYears(timePeriodYears);
        userRequest.setDreamType(dreamType != null ? dreamType : "Custom SIP What-If"); // Default dreamType for what-if scenarios
        userRequest = userRequestRepository.save(userRequest);

        // Get calculation from cache or Lambda
        // Operation: "calculate_fv_from_sip"
        // Parameters: futureValue is null, monthlySipAmount is provided
        LambdaSipResponse sipResponse = getOrCreateSipCalculationFromUserRequests(
                "calculate_fv_from_sip", null, timePeriodYears, monthlySipAmount);

        GoalCalculation goalCalculation = new GoalCalculation();
        goalCalculation.setRequestId(userRequest.getId());
        populateGoalCalculationWithSipResponse(goalCalculation, sipResponse);
        goalCalculationRepository.save(goalCalculation);

        return userRequest;
    }


    /**
     * Retrieves a specific UserRequest by its ID (which is the "Transaction ID").
     * @param requestId The UUID of the user request.
     * @return An Optional containing the UserRequest if found.
     */
    public Optional<UserRequest> getUserRequestById(UUID requestId) {
        return userRequestRepository.findById(requestId);
    }

    /**
     * Retrieves all UserRequests (transactions) for a specific user.
     * @param userId The UUID of the user.
     * @return A list of UserRequest objects associated with the user, typically ordered by timestamp (most recent first).
     */
    public List<UserRequest> getUserRequestsByUserId(UUID userId) {
        return userRequestRepository.findByUserId(userId);
    }

    /**
     * Retrieves the GoalCalculation (Table 2) associated with a specific UserRequest (Table 1).
     * @param requestId The UUID of the user request (Transaction ID).
     * @return An Optional containing the GoalCalculation if found.
     */
    public Optional<GoalCalculation> getGoalCalculationByRequestId(UUID requestId) {
        return goalCalculationRepository.findByRequestId(requestId);
    }

    /**
     * Helper method to retrieve SIP/FV calculation results.
     * This is the core caching logic:
     * 1. It first attempts to find existing calculation results in the database (using `user_requests` and `goal_calculations` as a cache).
     * 2. If no valid cached result is found, or if the cached result itself indicated an error, it calls the AWS Lambda function.
     *
     * @param operation The calculation type ("calculate_sip_from_fv" or "calculate_fv_from_sip"). This is important for cache key.
     * @param futureValue The target future value (used as part of cache key for "calculate_sip_from_fv").
     * @param timePeriodYears The time period in years (used as part of cache key).
     * @param monthlySipAmount The monthly SIP amount (used as part of cache key for "calculate_fv_from_sip").
     * @return A LambdaSipResponse object containing the SIP/FV calculation details.
     */
    private LambdaSipResponse getOrCreateSipCalculationFromUserRequests(String operation, BigDecimal futureValue, Integer timePeriodYears, BigDecimal monthlySipAmount) {
        LambdaSipResponse sipResponse = null;
        String lambdaResponseJson = null;

        // Determine the 'key' value for caching based on the operation
        BigDecimal cacheKeyAmount = null;
        if ("calculate_sip_from_fv".equals(operation)) {
            cacheKeyAmount = futureValue;
        } else if ("calculate_fv_from_sip".equals(operation)) {
            cacheKeyAmount = monthlySipAmount;
        } else {
            // Should not happen if validation is correct upstream
            logger.severe("Attempted to cache for an unknown operation: " + operation + ". Calling Lambda directly.");
            return sipCalculationService.performLambdaCalculation(operation, timePeriodYears, futureValue, monthlySipAmount);
        }

        // 1. Try to find an existing UserRequest that matches the cache key (amount and time_period_years)
        // We order by request timestamp ascending to consistently pick the "oldest" relevant cached entry.
        // This is our custom caching mechanism using the user_requests table.
        List<UserRequest> existingRequests = userRequestRepository
                .findByFutureValueAndTimePeriodYearsOrderByRequestTimestampAsc(cacheKeyAmount, timePeriodYears);

        if (!existingRequests.isEmpty()) {
            UserRequest cacheSourceRequest = existingRequests.get(0); // Use the oldest matching request as the source of cached calculation
            Optional<GoalCalculation> cachedGoalCalculation = goalCalculationRepository.findByRequestId(cacheSourceRequest.getId());

            if (cachedGoalCalculation.isPresent()) {
                // If a GoalCalculation exists for that request, attempt to use its stored JSON
                logger.info("Found calculation in user_requests/goal_calculations cache for operation " + operation + " for amount: " + cacheKeyAmount + ", Years: " + timePeriodYears + " (from request ID: " + cacheSourceRequest.getId() + ")");
                lambdaResponseJson = cachedGoalCalculation.get().getAwsLambdaResponseJson();
                try {
                    sipResponse = objectMapper.readValue(lambdaResponseJson, LambdaSipResponse.class);
                    // Critical check: Ensure the cached response itself didn't contain an error from a prior Lambda invocation
                    if (sipResponse != null && (sipResponse.getError() != null && !sipResponse.getError().isEmpty())) {
                        logger.warning("Cached calculation for request ID " + cacheSourceRequest.getId() + " itself contained a previous error. Recalculating with Lambda.");
                        sipResponse = null; // Force Lambda call if cached result was an error
                    }
                } catch (JsonProcessingException e) {
                    // If deserialization of the cached JSON fails (e.g., corrupted data), force recalculation
                    logger.severe("Failed to deserialize cached Lambda response JSON from goal_calculations for request ID " + cacheSourceRequest.getId() + ": " + e.getMessage() + ". Recalculating with Lambda.");
                    sipResponse = null;
                }
            } else {
                // This indicates an inconsistency: UserRequest exists, but its corresponding GoalCalculation is missing.
                logger.warning("User request found but no associated GoalCalculation for request ID: " + cacheSourceRequest.getId() + ". Forcing Lambda call.");
                sipResponse = null;
            }
        }

        // 2. If no valid cached result was found (or forced by corruption/error), call the AWS Lambda function
        if (sipResponse == null) {
            logger.info("No valid cache found. Calling AWS Lambda for operation " + operation + " for amount: " + cacheKeyAmount + ", Years: " + timePeriodYears);
            // This is the actual call to the external Lambda service
            sipResponse = sipCalculationService.performLambdaCalculation(operation, timePeriodYears, futureValue, monthlySipAmount);
        }
        return sipResponse;
    }

    /**
     * Helper method to populate a GoalCalculation entity with SIP/FV calculation results from a LambdaSipResponse.
     * This method handles populating either `monthlySipRequiredBestWeightedCase` OR `calculatedFutureValueFromUserSip`
     * based on which calculation was performed by Lambda.
     *
     * @param goalCalculation The GoalCalculation entity to populate.
     * @param sipResponse The LambdaSipResponse object received from calculation (cache or Lambda).
     */
    private void populateGoalCalculationWithSipResponse(GoalCalculation goalCalculation, LambdaSipResponse sipResponse) {
        if (sipResponse != null && (sipResponse.getError() == null || sipResponse.getError().isEmpty())) {
            // Populate common calculated field
            goalCalculation.setCalculatedBestWeightedAnnualRate(parsePercentageToBigDecimal(sipResponse.getCalculatedBestWeightedAnnualRate()));

            // Populate the specific result field based on which calculation type was performed by Lambda
            if (sipResponse.getMonthlySipRequiredBestWeightedCase() != null) {
                goalCalculation.setMonthlySipRequiredBestWeightedCase(sipResponse.getMonthlySipRequiredBestWeightedCase());
                goalCalculation.setCalculatedFutureValueFromUserSip(null); // Ensure the other mutually exclusive field is null
            } else if (sipResponse.getCalculatedFutureValue() != null) {
                goalCalculation.setCalculatedFutureValueFromUserSip(sipResponse.getCalculatedFutureValue());
                goalCalculation.setMonthlySipRequiredBestWeightedCase(null); // Ensure the other mutually exclusive field is null
            } else {
                // Log a warning if a successful Lambda response contained no primary calculation result.
                logger.warning("Lambda response was successful but contained no primary calculation result (monthly SIP or future value). Check Lambda logic.");
                goalCalculation.setMonthlySipRequiredBestWeightedCase(null);
                goalCalculation.setCalculatedFutureValueFromUserSip(null);
            }

            try {
                // Store the raw Lambda response JSON string into the GoalCalculation for audit/completeness
                goalCalculation.setAwsLambdaResponseJson(objectMapper.writeValueAsString(sipResponse));
            } catch (JsonProcessingException e) {
                logger.warning("Failed to serialize Lambda response for GoalCalculation entity: " + e.getMessage());
                // Fallback: Store a generic error JSON if serialization fails
                goalCalculation.setAwsLambdaResponseJson("{ \"error\": \"Failed to serialize Lambda response for calculation entity\" }");
            }
        } else {
            // If Lambda response was null or contained an error, log and store error message in JSON field
            logger.severe("SIP/FV calculation response for request was an error or null. GoalCalculation will be saved without detailed results.");
            String errorDetails = (sipResponse != null && sipResponse.getError() != null ? sipResponse.getError() : "Unknown error or null response from Lambda.");
            goalCalculation.setAwsLambdaResponseJson("{ \"error\": \"SIP/FV calculation failed\", \"details\": \"" + errorDetails + "\" }");
            // Explicitly ensure all calculation-related fields are null on error
            goalCalculation.setCalculatedBestWeightedAnnualRate(null);
            goalCalculation.setMonthlySipRequiredBestWeightedCase(null);
            goalCalculation.setCalculatedFutureValueFromUserSip(null);
        }
    }

    /**
     * Helper method to parse a percentage string (e.g., "15.20%") into a BigDecimal decimal (e.g., 0.1520).
     * @param percentageString The percentage string to parse.
     * @return The parsed BigDecimal value (e.g., 0.1520), or null if parsing fails or input is blank.
     */
    private BigDecimal parsePercentageToBigDecimal(String percentageString) {
        if (percentageString == null || percentageString.isBlank()) {
            return null;
        }
        try {
            String cleanString = percentageString.replace("%", "").trim();
            // Divide by 100 with a scale of 4 for precision (e.g., 0.1520 for 15.20%)
            return new BigDecimal(cleanString).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        } catch (NumberFormatException | ArithmeticException e) {
            logger.warning("Failed to parse percentage string: " + percentageString + " - " + e.getMessage());
            return null;
        }
    }
}