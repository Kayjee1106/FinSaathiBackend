package com.finsaathi.SipCalculator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsaathi.SipCalculator.model.LambdaSipRequest;
import com.finsaathi.SipCalculator.model.LambdaSipResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.math.BigDecimal;
import java.util.logging.Logger;

@Service // Marks this class as a Spring Service component
public class SipCalculationService {

    private static final Logger logger = Logger.getLogger(SipCalculationService.class.getName());

    @Value("${aws.lambda.sip.api.url}") // Injects the AWS Lambda API Gateway URL from application.properties
    private String lambdaApiUrl;

    private final RestTemplate restTemplate; // Spring's HTTP client for making REST calls

    @Autowired // Automatically injects an instance of ObjectMapper, configured in AppConfig
    private ObjectMapper objectMapper;

    /**
     * Constructor for dependency injection of RestTemplate.
     * @param restTemplate The RestTemplate instance provided by Spring's IoC container.
     */
    public SipCalculationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Performs a generic SIP calculation operation by calling the external AWS Lambda API.
     * This method constructs the request payload based on the operation and sends it to Lambda.
     *
     * @param operation The type of calculation requested ("calculate_sip_from_fv" or "calculate_fv_from_sip").
     * @param timeInYears The investment time horizon in years.
     * @param futureValue The target future value (pass null if operation is calculate_fv_from_sip).
     * @param monthlySipAmount The monthly SIP amount (pass null if operation is calculate_sip_from_fv).
     * @return A {@link LambdaSipResponse} object containing calculation results or error details from Lambda.
     */
    public LambdaSipResponse performLambdaCalculation(String operation, Integer timeInYears, BigDecimal futureValue, BigDecimal monthlySipAmount) {
        // Create the request payload object for Lambda
        LambdaSipRequest request = new LambdaSipRequest();
        request.setOperation(operation);
        request.setTimeInYears(timeInYears);

        // Populate the specific input field based on the requested operation
        if ("calculate_sip_from_fv".equals(operation)) {
            request.setFutureValue(futureValue);
        } else if ("calculate_fv_from_sip".equals(operation)) {
            request.setMonthlySipAmount(monthlySipAmount);
        } else {
            // This case should ideally be caught by validation earlier in the controller/service layer
            // but acts as a safeguard.
            LambdaSipResponse response = new LambdaSipResponse();
            response.setError("Invalid internal operation type provided to SipCalculationService.");
            response.setDetails("Operation: " + operation + " is not supported.");
            return response;
        }

        // Set HTTP headers for the request (Content-Type: application/json is crucial for JSON APIs)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Create the HTTP entity (combining the request body and headers)
        HttpEntity<LambdaSipRequest> entity = new HttpEntity<>(request, headers);

        try {
            // Log the outgoing request for debugging
            logger.info("Calling Lambda API: " + lambdaApiUrl + " with operation: " + operation + ", request: " + objectMapper.writeValueAsString(request));

            // Send POST request to the Lambda API URL and map the JSON response to LambdaSipResponse
            return restTemplate.postForObject(lambdaApiUrl, entity, LambdaSipResponse.class);

        } catch (HttpClientErrorException e) {
            // Handles 4xx client errors (e.g., 400 Bad Request if Lambda's validation fails)
            logger.severe("Client error calling Lambda: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            try {
                // Attempt to parse Lambda's error response into our LambdaSipResponse model.
                // This assumes Lambda returns a structured JSON error that can be deserialized.
                return objectMapper.readValue(e.getResponseBodyAsString(), LambdaSipResponse.class);
            } catch (Exception parseException) {
                // Fallback if Lambda's error response cannot be parsed (e.g., malformed JSON)
                logger.severe("Failed to parse Lambda client error response body: " + parseException.getMessage());
                LambdaSipResponse response = new LambdaSipResponse();
                response.setError("Failed to parse Lambda client error response.");
                response.setDetails(e.getStatusCode() + ": " + e.getResponseBodyAsString());
                return response;
            }
        } catch (HttpServerErrorException e) {
            // Handles 5xx server errors (e.g., 500 Internal Server Error within Lambda itself)
            logger.severe("Server error calling Lambda: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            LambdaSipResponse response = new LambdaSipResponse();
            response.setError("Lambda internal server error.");
            response.setDetails(e.getStatusCode() + ": " + e.getResponseBodyAsString());
            return response;
        } catch (Exception e) {
            // Catches any other unexpected exceptions during the API call (e.g., network connectivity issues, DNS errors)
            logger.severe("An unexpected error occurred while calling Lambda API: " + e.getMessage());
            LambdaSipResponse response = new LambdaSipResponse();
            response.setError("An unexpected error occurred while calling Lambda.");
            response.setDetails(e.getMessage());
            return response;
        }
    }
}
