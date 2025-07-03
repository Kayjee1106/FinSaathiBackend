package com.finsaathi.SipCalculator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsaathi.SipCalculator.model.*;
import com.finsaathi.SipCalculator.repository.InvestmentSchemeRateRepository;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb; // NEW IMPORT for colors
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.action.PdfAction;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.properties.VerticalAlignment;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

@Service
public class PdfGeneratorService {

    private static final Logger logger = Logger.getLogger(PdfGeneratorService.class.getName());
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Define the FinSaathi blue color (from the image, looks like RGB 0, 102, 204)
    private static final DeviceRgb FINSAATHI_BLUE = new DeviceRgb(0, 102, 204);
    private static final DeviceRgb LIGHT_GREY = new DeviceRgb(240, 240, 240); // For table headers

    /**
     * Generates a PDF report summarizing a user's request, associated calculation, and allocated scheme suggestions.
     * The PDF layout is designed to match the provided image, with new recommendations.
     * @param user The User entity.
     * @param userRequest The UserRequest entity.
     * @param goalCalculation The GoalCalculation entity.
     * @param allocatedSuggestions List of SchemeSipSuggestion representing the 50-30-20 allocation breakdown.
     * @return A byte array containing the generated PDF document.
     * @throws IOException if an error occurs during PDF generation.
     */
    public byte[] generatePdfReport(User user, UserRequest userRequest, GoalCalculation goalCalculation, List<SchemeSipSuggestion> allocatedSuggestions) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.setMargins(50, 50, 50, 50);

        DecimalFormat currencyFormat = new DecimalFormat("'Rs.' #,##0");
        DecimalFormat percentageFormat = new DecimalFormat("#,##0.0'%'");

        // --- Header Section (with Logo and Title) ---
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{1, 5})).setWidth(UnitValue.createPercentValue(100));

        // Add Logo
        try {
            String logoImageUrl = "https://my-finsaathi-public-assets.s3.ap-south-1.amazonaws.com/FinSaathi+Logo+Updated.png"; // REMEMBER TO UPDATE THIS URL
            Image logo = new Image(ImageDataFactory.create(logoImageUrl));
            logo.setWidth(UnitValue.createPointValue(50));
            logo.setHeight(UnitValue.createPointValue(50));
            headerTable.addCell(new Cell().add(logo).setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE));
        } catch (Exception e) {
            logger.warning("Could not load logo image: " + e.getMessage());
            headerTable.addCell(new Cell().add(new Paragraph("")).setBorder(Border.NO_BORDER));
        }

        headerTable.addCell(new Cell().add(new Paragraph("FinSaathi Goal Planning Report")
                        .setFontSize(24)
                        .setTextAlignment(TextAlignment.LEFT) // Adjusted to left-align with image, as per image
                        .setBold()
                        .setFontColor(FINSAATHI_BLUE))
                .setBorder(Border.NO_BORDER));
        document.add(headerTable.setMarginBottom(5));

        Table underlineTable = new Table(UnitValue.createPercentArray(1)).setWidth(UnitValue.createPercentValue(100));
        underlineTable.addCell(new Cell().setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(FINSAATHI_BLUE, 2)).setHeight(2));
        document.add(underlineTable.setMarginBottom(20));

        document.add(new Paragraph("(Prepared for: " + user.getName() + ")")
                .setFontSize(12)
                .setMarginBottom(5));
        document.add(new Paragraph("Date: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")))
                .setFontSize(12)
                .setMarginBottom(30)); // Increased margin for section separation

        // --- Goal Summary Section ---
        document.add(new Paragraph("Goal Summary")
                .setFontSize(18)
                .setBold()
                .setFontColor(FINSAATHI_BLUE)
                .setMarginBottom(10));

        Table goalSummaryTable = new Table(UnitValue.createPercentArray(new float[]{1, 2})).setWidth(UnitValue.createPercentValue(60));
        goalSummaryTable.addCell(createGoalSummaryLabelCell("Goal Type:"));
        goalSummaryTable.addCell(createGoalSummaryValueCell(userRequest.getDreamType() != null ? userRequest.getDreamType() : "N/A"));

        goalSummaryTable.addCell(createGoalSummaryLabelCell("Goal Time Horizon:"));
        goalSummaryTable.addCell(createGoalSummaryValueCell(userRequest.getTimePeriodYears() + " Years"));

        BigDecimal userOriginalTargetFV = userRequest.getOriginalTargetFutureValue();
        BigDecimal monthlyInvestmentForSummary;
        if (goalCalculation.getMonthlySipRequiredBestWeightedCase() != null) {
            monthlyInvestmentForSummary = goalCalculation.getMonthlySipRequiredBestWeightedCase();
        } else if (userRequest.getFutureValue() != null) {
            monthlyInvestmentForSummary = userRequest.getFutureValue();
        } else {
            monthlyInvestmentForSummary = BigDecimal.ZERO;
        }

        goalSummaryTable.addCell(createGoalSummaryLabelCell("Target Amount:"));
        goalSummaryTable.addCell(createGoalSummaryValueCell(userOriginalTargetFV != null ? currencyFormat.format(userOriginalTargetFV) : "N/A"));

        goalSummaryTable.addCell(createGoalSummaryLabelCell("Monthly Investment:"));
        goalSummaryTable.addCell(createGoalSummaryValueCell(monthlyInvestmentForSummary != null ? currencyFormat.format(monthlyInvestmentForSummary) : "N/A"));

        goalSummaryTable.addCell(createGoalSummaryLabelCell("Investment Type:"));
        goalSummaryTable.addCell(createGoalSummaryValueCell("Diversified"));

        document.add(goalSummaryTable.setMarginBottom(30));

        // --- Projection Summary Section ---
        document.add(new Paragraph("Projection Summary")
                .setFontSize(18)
                .setBold()
                .setFontColor(FINSAATHI_BLUE)
                .setMarginBottom(10));

        Table projectionTable = new Table(UnitValue.createPercentArray(new float[]{2, 1, 2})).setWidth(UnitValue.createPercentValue(100));
        projectionTable.addHeaderCell(createHeaderCell("Investment Type"));
        projectionTable.addHeaderCell(createHeaderCell("Return Rate"));
        projectionTable.addHeaderCell(createHeaderCell("Expected Value (" + userRequest.getTimePeriodYears() + "Y)*"));

        Map<String, Double> lambdaRateBreakdown = new HashMap<>();
        Map<String, BigDecimal> ratesFromLambda = null;
        try {
            Map<String, Object> lambdaResponseMap = objectMapper.readValue(goalCalculation.getAwsLambdaResponseJson(), new TypeReference<Map<String, Object>>() {});
            ratesFromLambda = (Map<String, BigDecimal>) lambdaResponseMap.get("rate_breakdown");

            if (ratesFromLambda != null) {
                ratesFromLambda.forEach((key, value) -> lambdaRateBreakdown.put(key, value.doubleValue()));
            }
        } catch (JsonProcessingException | ClassCastException e) {
            logger.severe("Failed to parse Lambda JSON for rate breakdown in PDF: " + e.getMessage());
        }

        BigDecimal actualFVFromUserSIP = goalCalculation.getCalculatedFutureValueFromUserSip();
        BigDecimal blendedRate = goalCalculation.getCalculatedBestWeightedAnnualRate();

        if (monthlyInvestmentForSummary != null && monthlyInvestmentForSummary.compareTo(BigDecimal.ZERO) > 0) {
            Optional<SchemeSipSuggestion> mfSuggestion = allocatedSuggestions.stream()
                    .filter(s -> s.getProductType().equals("Mutual funds"))
                    .findFirst();
            mfSuggestion.ifPresent(s -> {
                BigDecimal expectedValue = calculateFVFromSIP(s.getMonthlySipRequiredForGoal(), userRequest.getTimePeriodYears() * 12, s.getAnnualRate().doubleValue());
                projectionTable.addCell(createDataCell(s.getProductType() + " (SIP)")); // Match image "Mutual Funds (SIP)"
                projectionTable.addCell(createDataCell(percentageFormat.format(s.getAnnualRate().multiply(new BigDecimal("100")))));
                projectionTable.addCell(createDataCell(currencyFormat.format(expectedValue)));
            });

            Optional<SchemeSipSuggestion> bankSuggestion = allocatedSuggestions.stream()
                    .filter(s -> s.getProductType().equals("Bank Savings Products"))
                    .findFirst();
            bankSuggestion.ifPresent(s -> {
                BigDecimal expectedValue = calculateFVFromSIP(s.getMonthlySipRequiredForGoal(), userRequest.getTimePeriodYears() * 12, s.getAnnualRate().doubleValue());
                projectionTable.addCell(createDataCell("Bank Fixed Deposit")); // Match image "Bank Fixed Deposit"
                projectionTable.addCell(createDataCell(percentageFormat.format(s.getAnnualRate().multiply(new BigDecimal("100")))));
                projectionTable.addCell(createDataCell(currencyFormat.format(expectedValue)));
            });

            Optional<SchemeSipSuggestion> equityGoldSuggestion = allocatedSuggestions.stream()
                    .filter(s -> s.getProductType().equals("Equity") || s.getProductType().equals("Gold"))
                    .findFirst();
            equityGoldSuggestion.ifPresent(s -> {
                BigDecimal expectedValue = calculateFVFromSIP(s.getMonthlySipRequiredForGoal(), userRequest.getTimePeriodYears() * 12, s.getAnnualRate().doubleValue());
                projectionTable.addCell(createDataCell("Equity Shares")); // Match image "Equity Shares"
                projectionTable.addCell(createDataCell(percentageFormat.format(s.getAnnualRate().multiply(new BigDecimal("100")))));
                projectionTable.addCell(createDataCell(currencyFormat.format(expectedValue)));
            });

            BigDecimal blendedPortfolioProjectedFV = BigDecimal.ZERO;
            if (blendedRate != null) {
                if(goalCalculation.getCalculatedFutureValueFromUserSip() != null) {
                    blendedPortfolioProjectedFV = goalCalculation.getCalculatedFutureValueFromUserSip();
                } else {
                    blendedPortfolioProjectedFV = calculateFVFromSIP(monthlyInvestmentForSummary, userRequest.getTimePeriodYears() * 12, blendedRate.doubleValue());
                }

                projectionTable.addCell(createDataCell("Blended Portfolio"));
                projectionTable.addCell(createDataCell(percentageFormat.format(blendedRate.multiply(new BigDecimal("100")))));
                projectionTable.addCell(createDataCell(currencyFormat.format(blendedPortfolioProjectedFV)));
            } else {
                projectionTable.addCell(createDataCell("Blended Portfolio"));
                projectionTable.addCell(createDataCell("N/A"));
                projectionTable.addCell(createDataCell("N/A"));
            }
        } else {
            // Placeholder rows if no valid monthly investment for projection
            projectionTable.addCell(createDataCell("N/A")); projectionTable.addCell(createDataCell("N/A")); projectionTable.addCell(createDataCell("N/A"));
            projectionTable.addCell(createDataCell("N/A")); projectionTable.addCell(createDataCell("N/A")); projectionTable.addCell(createDataCell("N/A"));
            projectionTable.addCell(createDataCell("N/A")); projectionTable.addCell(createDataCell("N/A")); projectionTable.addCell(createDataCell("N/A"));
        }

        document.add(projectionTable.setMarginBottom(10));

        BigDecimal shortfall = BigDecimal.ZERO;
        if (userOriginalTargetFV != null && actualFVFromUserSIP != null) {
            shortfall = userOriginalTargetFV.subtract(actualFVFromUserSIP);
            if (shortfall.compareTo(BigDecimal.ZERO) < 0) {
                shortfall = BigDecimal.ZERO;
            }
        } else if (userOriginalTargetFV != null && goalCalculation.getMonthlySipRequiredBestWeightedCase() != null) {
            shortfall = BigDecimal.ZERO;
        }

        document.add(new Paragraph("Shortfall to Goal: " + currencyFormat.format(shortfall))
                .setFontSize(12)
                .setBold()
                .setMarginBottom(5));
        document.add(new Paragraph("* Projections are indicative and based on input data. Actual outcomes may vary with market conditions.")
                .setFontSize(10)
                .setMarginBottom(30));


        // --- Recommendations Section ---
        document.add(new Paragraph("Recommendations")
                .setFontSize(18)
                .setBold()
                .setFontColor(FINSAATHI_BLUE)
                .setMarginBottom(10));

        if (userOriginalTargetFV != null && actualFVFromUserSIP != null && actualFVFromUserSIP.compareTo(userOriginalTargetFV) < 0) {
            BigDecimal requiredSipToHitTarget = calculateSIPFromFV(
                    userOriginalTargetFV,
                    userRequest.getTimePeriodYears() * 12,
                    blendedRate != null ? blendedRate.doubleValue() : 0.0
            );
            document.add(new Paragraph("- Increase investment to " + currencyFormat.format(requiredSipToHitTarget) + "/month for goal completion.")
                    .setFontSize(12)
                    .setMarginBottom(5));
        } else if (userOriginalTargetFV != null && actualFVFromUserSIP != null && actualFVFromUserSIP.compareTo(userOriginalTargetFV) >= 0) {
            document.add(new Paragraph("- Congratulations! Your current monthly investment is projected to meet or exceed your target goal.")
                    .setFontSize(12)
                    .setMarginBottom(5));
        } else {
            document.add(new Paragraph("- Review your current investment plan to ensure it aligns with your goals.")
                    .setFontSize(12)
                    .setMarginBottom(5));
        }

        if (userOriginalTargetFV != null && monthlyInvestmentForSummary != null && monthlyInvestmentForSummary.compareTo(BigDecimal.ZERO) > 0 && blendedRate != null && actualFVFromUserSIP != null && actualFVFromUserSIP.compareTo(userOriginalTargetFV) < 0) {
            int currentMonths = userRequest.getTimePeriodYears() * 12;
            int additionalMonths = calculateMonthsToAchieveGoal(
                    userOriginalTargetFV,
                    monthlyInvestmentForSummary,
                    blendedRate != null ? blendedRate.doubleValue() : 0.0,
                    currentMonths
            );

            if (additionalMonths > currentMonths) {
                int additionalYears = (int) Math.ceil((double)(additionalMonths - currentMonths) / 12.0);
                document.add(new Paragraph("- Alternatively, extend your timeline by approximately " + additionalYears + " more years at the current monthly investment rate to achieve your goal.")
                        .setFontSize(12)
                        .setMarginBottom(30));
            } else {
                document.add(new Paragraph("- Your current monthly investment is projected to meet your goal within the specified timeline.")
                        .setFontSize(12)
                        .setMarginBottom(30));
            }
        } else {
            document.add(new Paragraph("- Consider adjusting your investment strategy or timeline for optimal results.")
                    .setFontSize(12)
                    .setMarginBottom(30));
        }


        // --- Disclaimer Section ---
        document.add(new Paragraph("Disclaimer")
                .setFontSize(18)
                .setBold()
                .setFontColor(FINSAATHI_BLUE)
                .setMarginBottom(10));
        document.add(new Paragraph("“FinSaathi (A brand of Santhani financial Services) is a registered distributor of mutual funds, insurance products, and government saving schemes. All projections are based on historical market performance and may vary due to market volatility. Please consult the founder of FinSaathi for further details.")
                .setFontSize(10)
                .setMarginBottom(30));

        // --- Footer Section ---
        document.add(new Paragraph("Founder Name: Dr B Sekar")
                .setFontSize(10)
                .setMarginBottom(5));
        // Corrected: Use Link element for clickable email
        document.add(new Paragraph().add(new Text("Email ID: ").setFontSize(10))
                .add(new Link("santhanifinancials@gmail.com", PdfAction.createURI("mailto:santhanifinancials@gmail.com")))
                .setFontSize(10)
                .setMarginBottom(5));
        document.add(new Paragraph("Social Media: @santhaniFinancialservices")
                .setFontSize(10)
                .setMarginBottom(5));


        document.close();
        return baos.toByteArray();
    }

    /** Helper to create a styled header cell for tables. */
    private Cell createHeaderCell(String content) {
        return new Cell().add(new Paragraph(content).setBold())
                .setBackgroundColor(LIGHT_GREY) // Use defined light grey color
                .setBorder(new SolidBorder(DeviceRgb.BLACK, 0.5f))
                .setTextAlignment(TextAlignment.CENTER);
    }

    /** Helper to create a styled data cell for tables. */
    private Cell createDataCell(String content) {
        return new Cell().add(new Paragraph(content))
                .setBorder(new SolidBorder(DeviceRgb.BLACK, 0.5f))
                .setTextAlignment(TextAlignment.CENTER);
    }

    /** Helper to create a styled label cell for Goal Summary table. */
    private Cell createGoalSummaryLabelCell(String content) {
        return new Cell().add(new Paragraph(content).setBold().setFontSize(12)) // Bold, smaller font
                .setBorder(Border.NO_BORDER);
    }

    /** Helper to create a styled value cell for Goal Summary table. */
    private Cell createGoalSummaryValueCell(String content) {
        return new Cell().add(new Paragraph(content).setFontSize(12)) // Regular font, smaller size
                .setBorder(Border.NO_BORDER);
    }

    /** Helper method to format a BigDecimal amount into a currency string. */
    private String formatCurrency(BigDecimal amount) {
        DecimalFormat df = new DecimalFormat("'Rs.' #,##0");
        return df.format(amount);
    }

    /**
     * Replicates the calculateFVFromSIP logic from Lambda for PDF internal use.
     * FV = P * ( [((1 + i)^n - 1) / i] * (1 + i) )
     * @param monthlySipAmount The monthly SIP amount (P).
     * @param timeInMonths The total number of investment periods in months (n).
     * @param annualRate The annual interest rate (used to derive periodic rate i).
     * @return The calculated future value as BigDecimal, rounded to 2 decimal places.
     */
    private BigDecimal calculateFVFromSIP(BigDecimal monthlySipAmount, int timeInMonths, double annualRate) {
        if (timeInMonths <= 0) return monthlySipAmount.setScale(2, RoundingMode.HALF_UP);
        if (annualRate <= 0) {
            return monthlySipAmount.multiply(BigDecimal.valueOf(timeInMonths)).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal monthlyRate = BigDecimal.valueOf(annualRate).divide(BigDecimal.valueOf(12.0), 10, RoundingMode.HALF_UP);
        BigDecimal compoundFactor = monthlyRate.add(BigDecimal.ONE).pow(timeInMonths);

        BigDecimal futureValueFactor = (compoundFactor.subtract(BigDecimal.ONE))
                .divide(monthlyRate, 10, RoundingMode.HALF_UP)
                .multiply(monthlyRate.add(BigDecimal.ONE));

        return monthlySipAmount.multiply(futureValueFactor).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Replicates the calculateSIPFromFV logic from Lambda for PDF internal use.
     * P = FV / ( [((1 + i)^n - 1) / i] * (1 + i) )
     * @param futureValue The target future value (FV).
     * @param timeInMonths The total number of investment periods in months (n).
     * @param annualRate The annual interest rate (used to derive periodic rate i).
     * @return The calculated monthly SIP amount as BigDecimal, rounded to 2 decimal places.
     */
    private BigDecimal calculateSIPFromFV(BigDecimal futureValue, int timeInMonths, double annualRate) {
        if (timeInMonths <= 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (annualRate <= 0) {
            return futureValue.divide(BigDecimal.valueOf(timeInMonths), 2, RoundingMode.HALF_UP);
        }

        BigDecimal monthlyRate = BigDecimal.valueOf(annualRate).divide(BigDecimal.valueOf(12.0), 10, RoundingMode.HALF_UP);
        BigDecimal compoundFactor = monthlyRate.add(BigDecimal.ONE).pow(timeInMonths);

        BigDecimal denominator = (compoundFactor.subtract(BigDecimal.ONE))
                .divide(monthlyRate, 10, RoundingMode.HALF_UP)
                .multiply(monthlyRate.add(BigDecimal.ONE));

        return futureValue.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    /**
     * Iteratively calculates the number of months required to achieve a goal
     * with a given monthly SIP and annual rate.
     * @param targetFutureValue The target future value.
     * @param monthlySip The monthly investment amount.
     * @param annualRate The annual interest rate.
     * @param startMonths The current number of months already considered (to avoid infinite loops for already met goals).
     * @return The total number of months required, or a large number (Integer.MAX_VALUE) if not achievable within reasonable limits.
     */
    private int calculateMonthsToAchieveGoal(BigDecimal targetFutureValue, BigDecimal monthlySip, double annualRate, int startMonths) {
        if (monthlySip.compareTo(BigDecimal.ZERO) <= 0 || annualRate <= 0) {
            return Integer.MAX_VALUE;
        }
        if (targetFutureValue.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        BigDecimal monthlyRate = BigDecimal.valueOf(annualRate).divide(BigDecimal.valueOf(12.0), 10, RoundingMode.HALF_UP);
        int months = Math.max(1, startMonths);
        int maxMonths = 30 * 12;

        while (months <= maxMonths) {
            BigDecimal projectedFV = calculateFVFromSIP(monthlySip, months, annualRate);
            if (projectedFV.compareTo(targetFutureValue) >= 0) {
                return months;
            }
            months++;
        }
        return Integer.MAX_VALUE;
    }
}