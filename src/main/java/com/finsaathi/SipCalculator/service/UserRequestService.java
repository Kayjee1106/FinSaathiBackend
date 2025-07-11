package com.finsaathi.SipCalculator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsaathi.SipCalculator.model.*;
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

@Service
public class UserRequestService {

    private static final Logger logger = Logger.getLogger(UserRequestService.class.getName());

    @Autowired
    private UserRequestRepository userRequestRepository;

    @Autowired
    private GoalCalculationRepository goalCalculationRepository;

    @Autowired
    private SipCalculationService sipCalculationService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Creates and saves a new user request for a goal calculation (SIP from FV).
     * This is the primary flow where user provides a Future Value goal and wants to find the Monthly SIP.
     * @param requestDto The DTO containing all input parameters.
     * @return The newly created UserRequest object.
     */
    @Transactional
    public UserRequest createNewUserRequestAndCalculateSip(CalculateAndSaveRequest requestDto) {
        UserRequest userRequest = new UserRequest();
        userRequest.setUserId(requestDto.getUserId());
        userRequest.setFutureValue(requestDto.getFutureValue()); // CORRECT: futureValue is the goal here
        userRequest.setTimePeriodYears(requestDto.getTimePeriodYears());
        userRequest.setDreamType(requestDto.getDreamType());
        userRequest.setOriginalTargetFutureValue(requestDto.getFutureValue()); // CORRECT: Original target is the futureValue for this flow
        userRequest = userRequestRepository.save(userRequest);

        LambdaSipResponse sipResponse = getOrCreateSipCalculationFromUserRequests(
                "calculate_sip_from_fv", requestDto.getFutureValue(), requestDto.getTimePeriodYears(), null);

        GoalCalculation goalCalculation = new GoalCalculation();
        goalCalculation.setRequestId(userRequest.getId());
        populateGoalCalculationWithSipResponse(goalCalculation, sipResponse);
        goalCalculationRepository.save(goalCalculation);

        return userRequest;
    }

    /**
     * Creates and saves a new user request for a Future Value calculation from a custom Monthly SIP.
     * This is for the "what-if" scenario where user provides a Monthly SIP and gets FV.
     * It now correctly stores the user's original target future value and the monthly SIP.
     * @param requestDto The DTO containing all input parameters.
     * @return The newly created UserRequest object.
     */
    @Transactional
    public UserRequest createFutureValueCalculationForUserSip(CalculateFvFromSipRequest requestDto) {
        UserRequest userRequest = new UserRequest();
        userRequest.setUserId(requestDto.getUserId());
        userRequest.setFutureValue(requestDto.getMonthlySipAmount()); // CORRECT: futureValue stores the monthly SIP input for this request type
        userRequest.setTimePeriodYears(requestDto.getTimePeriodYears());
        userRequest.setDreamType(requestDto.getDreamType() != null ? requestDto.getDreamType() : "Custom SIP What-If");
        userRequest.setOriginalTargetFutureValue(requestDto.getOriginalTargetFutureValue()); // CORRECT: Store the original target FV here
        userRequest = userRequestRepository.save(userRequest);

        LambdaSipResponse sipResponse = getOrCreateSipCalculationFromUserRequests(
                "calculate_fv_from_sip", requestDto.getOriginalTargetFutureValue(), requestDto.getTimePeriodYears(), requestDto.getMonthlySipAmount());

        GoalCalculation goalCalculation = new GoalCalculation();
        goalCalculation.setRequestId(userRequest.getId());
        populateGoalCalculationWithSipResponse(goalCalculation, sipResponse);
        goalCalculationRepository.save(goalCalculation);

        return userRequest;
    }


    /** Retrieves an existing user request by its ID. */
    public Optional<UserRequest> getUserRequestById(UUID requestId) {
        return userRequestRepository.findById(requestId);
    }

    /** Retrieves all user requests for a specific user. */
    public List<UserRequest> getUserRequestsByUserId(UUID userId) {
        return userRequestRepository.findByUserId(userId);
    }

    /** Retrieves the GoalCalculation (Table 2) associated with a specific UserRequest. */
    public Optional<GoalCalculation> getGoalCalculationByRequestId(UUID requestId) {
        return goalCalculationRepository.findByRequestId(requestId);
    }

    /**
     * Helper method to retrieve SIP/FV calculation results from existing user requests (as cache) or call Lambda.
     * It now uses the correct `futureValue` for `calculate_sip_from_fv` and `monthlySipAmount` for `calculate_fv_from_sip` as cache keys.
     */
    private LambdaSipResponse getOrCreateSipCalculationFromUserRequests(String operation, BigDecimal futureValue, Integer timePeriodYears, BigDecimal monthlySipAmount) {
        LambdaSipResponse sipResponse = null;
        String lambdaResponseJson = null;

        BigDecimal cacheKeyAmount = null;
        if ("calculate_sip_from_fv".equals(operation)) {
            cacheKeyAmount = futureValue; // Cache key is the target FV
        } else if ("calculate_fv_from_sip".equals(operation)) {
            cacheKeyAmount = monthlySipAmount; // Cache key is the monthly SIP input
        } else {
            logger.severe("Attempted to cache for an unknown operation: " + operation + ". Calling Lambda directly.");
            return sipCalculationService.performLambdaCalculation(operation, timePeriodYears, futureValue, monthlySipAmount);
        }

        List<UserRequest> existingRequests = userRequestRepository
                .findByFutureValueAndTimePeriodYearsOrderByRequestTimestampAsc(cacheKeyAmount, timePeriodYears);

        if (!existingRequests.isEmpty()) {
            UserRequest cacheSourceRequest = existingRequests.get(0);
            Optional<GoalCalculation> cachedGoalCalculation = goalCalculationRepository.findByRequestId(cacheSourceRequest.getId());

            if (cachedGoalCalculation.isPresent()) {
                logger.info("Found calculation in cache for operation " + operation + " for amount: " + cacheKeyAmount + ", Years: " + timePeriodYears + " (from request ID: " + cacheSourceRequest.getId() + ")");
                lambdaResponseJson = cachedGoalCalculation.get().getAwsLambdaResponseJson();
                try {
                    sipResponse = objectMapper.readValue(lambdaResponseJson, LambdaSipResponse.class);
                    if (sipResponse != null && (sipResponse.getError() != null && !sipResponse.getError().isEmpty())) {
                        logger.warning("Cached calculation for " + cacheSourceRequest.getId() + " itself contained an error. Recalculating with Lambda.");
                        sipResponse = null;
                    }
                } catch (JsonProcessingException e) {
                    logger.severe("Failed to deserialize cached Lambda response JSON from goal_calculations for " + cacheSourceRequest.getId() + ": " + e.getMessage() + ". Recalculating.");
                    sipResponse = null;
                }
            } else {
                logger.warning("User request found but no associated GoalCalculation for ID: " + cacheSourceRequest.getId() + ". Forcing Lambda call.");
                sipResponse = null;
            }
        }

        if (sipResponse == null) {
            logger.info("SIP calculation not found in user_requests cache. Calling AWS Lambda for operation " + operation + " for amount: " + cacheKeyAmount + ", Years: " + timePeriodYears);
            // Pass futureValue (original target) and monthlySipAmount (user's input) to Lambda
            sipResponse = sipCalculationService.performLambdaCalculation(operation, timePeriodYears, futureValue, monthlySipAmount);
        }
        return sipResponse;
    }

    /** Helper method to populate a GoalCalculation entity with calculation results from a LambdaSipResponse. */
    private void populateGoalCalculationWithSipResponse(GoalCalculation goalCalculation, LambdaSipResponse sipResponse) {
        if (sipResponse != null && (sipResponse.getError() == null || sipResponse.getError().isEmpty())) {
            goalCalculation.setCalculatedBestWeightedAnnualRate(parsePercentageToBigDecimal(sipResponse.getCalculatedBestWeightedAnnualRate()));

            if (sipResponse.getMonthlySipRequiredBestWeightedCase() != null) {
                goalCalculation.setMonthlySipRequiredBestWeightedCase(sipResponse.getMonthlySipRequiredBestWeightedCase());
                goalCalculation.setCalculatedFutureValueFromUserSip(null);
            } else if (sipResponse.getCalculatedFutureValue() != null) {
                goalCalculation.setCalculatedFutureValueFromUserSip(sipResponse.getCalculatedFutureValue());
                goalCalculation.setMonthlySipRequiredBestWeightedCase(null);
            } else {
                logger.warning("Lambda response was successful but contained no primary calculation result. Check Lambda logic.");
                goalCalculation.setMonthlySipRequiredBestWeightedCase(null);
                goalCalculation.setCalculatedFutureValueFromUserSip(null);
            }

            try {
                goalCalculation.setAwsLambdaResponseJson(objectMapper.writeValueAsString(sipResponse));
            } catch (JsonProcessingException e) {
                logger.warning("Failed to serialize Lambda response for GoalCalculation entity: " + e.getMessage());
                goalCalculation.setAwsLambdaResponseJson("{ \"error\": \"Failed to serialize Lambda response for calculation entity\" }");
            }
        } else {
            logger.severe("SIP/FV calculation response for request was an error or null. GoalCalculation will be saved without detailed results.");
            String errorDetails = (sipResponse != null && sipResponse.getError() != null ? sipResponse.getError() : "Unknown error or null response from Lambda.");
            goalCalculation.setAwsLambdaResponseJson("{ \"error\": \"SIP/FV calculation failed\", \"details\": \"" + errorDetails + "\" }");
            goalCalculation.setCalculatedBestWeightedAnnualRate(null);
            goalCalculation.setMonthlySipRequiredBestWeightedCase(null);
            goalCalculation.setCalculatedFutureValueFromUserSip(null);
        }
    }

    /** Helper method to parse a percentage string to BigDecimal decimal. */
    private BigDecimal parsePercentageToBigDecimal(String percentageString) {
        if (percentageString == null || percentageString.isBlank()) return null;
        try {
            return new BigDecimal(percentageString.replace("%", "").trim())
                    .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        } catch (NumberFormatException | ArithmeticException e) {
            logger.warning("Failed to parse percentage string: " + percentageString + " - " + e.getMessage());
            return null;
        }
    }
}