package com.finsaathi.SipCalculator.service;

import com.finsaathi.SipCalculator.model.GoalCalculation;
import com.finsaathi.SipCalculator.model.SchemeSipSuggestion;
import com.finsaathi.SipCalculator.model.User;
import com.finsaathi.SipCalculator.model.UserRequest;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;
import java.util.logging.Logger;

@Service
public class PdfGeneratorService {

    private static final Logger logger = Logger.getLogger(PdfGeneratorService.class.getName());

    /**
     * Generates a PDF report summarizing a user's request, associated calculation, and allocated scheme suggestions.
     * @param user The User entity, providing user name for the report.
     * @param userRequest The UserRequest entity (Table 1 data), providing dream type, future value, time period.
     * @param goalCalculation The GoalCalculation entity (Table 2 data), providing the calculated SIP scenarios.
     * @param allocatedSuggestions List of SchemeSipSuggestion representing the 50-30-20 allocation breakdown.
     * @return A byte array containing the generated PDF document.
     * @throws IOException if an error occurs during PDF generation.
     */
    public byte[] generatePdfReport(User user, UserRequest userRequest, GoalCalculation goalCalculation, List<SchemeSipSuggestion> allocatedSuggestions) throws IOException {
        // ByteArrayOutputStream will hold the PDF content in memory
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.setMargins(50, 50, 50, 50); // Set page margins (top, right, bottom, left)

        // --- Report Title ---
        document.add(new Paragraph("SIP Goal Tracking Report")
                .setFontSize(24)
                .setTextAlignment(TextAlignment.CENTER)
                .setBold()
                .setMarginBottom(20));

        // --- User and Request Summary Section ---
        document.add(new Paragraph("Report for: " + user.getName())
                .setFontSize(14)
                .setMarginBottom(5));
        document.add(new Paragraph("Dream Type: " + (userRequest.getDreamType() != null ? userRequest.getDreamType() : "N/A"))
                .setFontSize(14)
                .setMarginBottom(5));

        // Display Target Goal Value or User Provided Monthly Investment based on the calculation type
        if (goalCalculation.getMonthlySipRequiredBestWeightedCase() != null) {
            document.add(new Paragraph("Target Goal Value: ₹" + formatCurrency(userRequest.getFutureValue()))
                    .setFontSize(14)
                    .setMarginBottom(5));
        } else if (goalCalculation.getCalculatedFutureValueFromUserSip() != null) {
            document.add(new Paragraph("User Provided Monthly Investment: ₹" + formatCurrency(userRequest.getFutureValue())) // UserRequest.future_value stores the monthly SIP here
                    .setFontSize(14)
                    .setMarginBottom(5));
        }

        document.add(new Paragraph("Time Period: " + userRequest.getTimePeriodYears() + " years")
                .setFontSize(14)
                .setMarginBottom(20));
        document.add(new Paragraph("Request ID (Transaction ID): " + userRequest.getId().toString())
                .setFontSize(10)
                .setMarginBottom(20)
                .setTextAlignment(TextAlignment.RIGHT));


        // --- Calculated Investment Scenario Summary Table ---
        DecimalFormat currencyFormat = new DecimalFormat("#,##0.00");
        DecimalFormat percentageFormat = new DecimalFormat("#,##0.00'%'");

        document.add(new Paragraph("Calculated Investment Scenario:")
                .setFontSize(18)
                .setBold()
                .setMarginBottom(10));

        Table sipCalcTable = new Table(UnitValue.createPercentArray(new float[]{1, 2})); // Two columns: Description and Value
        sipCalcTable.setWidth(UnitValue.createPercentValue(100));
        sipCalcTable.addHeaderCell(new Paragraph("Description").setBold());
        sipCalcTable.addHeaderCell(new Paragraph("Value").setBold().setTextAlignment(TextAlignment.RIGHT));

        // Row for Best Weighted Annual Rate
        sipCalcTable.addCell(new Paragraph("Estimated Annual Return Rate (Weighted Average)"));
        sipCalcTable.addCell(new Paragraph(goalCalculation.getCalculatedBestWeightedAnnualRate() != null ? percentageFormat.format(goalCalculation.getCalculatedBestWeightedAnnualRate().multiply(new BigDecimal("100"))) : "N/A").setTextAlignment(TextAlignment.RIGHT));

        // Row for the primary calculated result (SIP from FV OR FV from SIP)
        if (goalCalculation.getMonthlySipRequiredBestWeightedCase() != null) {
            sipCalcTable.addCell(new Paragraph("Required Monthly SIP to reach Goal"));
            sipCalcTable.addCell(new Paragraph("₹" + currencyFormat.format(goalCalculation.getMonthlySipRequiredBestWeightedCase())).setTextAlignment(TextAlignment.RIGHT));
        } else if (goalCalculation.getCalculatedFutureValueFromUserSip() != null) {
            sipCalcTable.addCell(new Paragraph("Projected Future Value for Monthly Investment"));
            sipCalcTable.addCell(new Paragraph("₹" + currencyFormat.format(goalCalculation.getCalculatedFutureValueFromUserSip())).setTextAlignment(TextAlignment.RIGHT));
        } else {
            sipCalcTable.addCell(new Paragraph("Primary Calculation Result")); // Fallback
            sipCalcTable.addCell(new Paragraph("N/A").setTextAlignment(TextAlignment.RIGHT));
        }

        document.add(sipCalcTable.setMarginBottom(30));


        // --- Optimal Monthly SIP Allocation (50-30-20 Principle) Section ---
        document.add(new Paragraph("Optimal Monthly SIP Allocation (50-30-20 Principle):")
                .setFontSize(18)
                .setBold()
                .setMarginBottom(10));

        // Determine the overall optimal monthly SIP for this section
        BigDecimal overallOptimalMonthlySip = null;
        if (goalCalculation.getMonthlySipRequiredBestWeightedCase() != null) {
            overallOptimalMonthlySip = goalCalculation.getMonthlySipRequiredBestWeightedCase();
        } else if (userRequest.getFutureValue() != null) {
            overallOptimalMonthlySip = userRequest.getFutureValue(); // For calculate_fv_from_sip, userRequest.future_value stores the monthly SIP
        }

        if (overallOptimalMonthlySip != null && overallOptimalMonthlySip.compareTo(BigDecimal.ZERO) > 0) {
            document.add(new Paragraph("Based on a total optimal monthly investment of: ₹" + formatCurrency(overallOptimalMonthlySip) +
                    " for " + userRequest.getTimePeriodYears() + " years, allocate as follows:")
                    .setFontSize(12)
                    .setMarginBottom(10));
        } else {
            document.add(new Paragraph("Optimal monthly investment amount is not available for allocation breakdown.")
                    .setFontSize(12)
                    .setMarginBottom(10));
        }


        if (allocatedSuggestions.isEmpty()) {
            document.add(new Paragraph("No optimal scheme allocations found for this time period in our database."));
        } else {
            Table allocatedTable = new Table(UnitValue.createPercentArray(new float[]{1, 2, 1, 1})); // Category, Scheme, Rate, Allocated SIP
            allocatedTable.setWidth(UnitValue.createPercentValue(100));
            allocatedTable.addHeaderCell(new Paragraph("Category").setBold());
            allocatedTable.addHeaderCell(new Paragraph("Best Scheme Name").setBold());
            allocatedTable.addHeaderCell(new Paragraph("Scheme Rate").setBold().setTextAlignment(TextAlignment.RIGHT));
            allocatedTable.addHeaderCell(new Paragraph("Allocated Monthly SIP").setBold().setTextAlignment(TextAlignment.RIGHT));

            for (SchemeSipSuggestion suggestion : allocatedSuggestions) {
                allocatedTable.addCell(new Paragraph(suggestion.getProductType()));
                allocatedTable.addCell(new Paragraph(suggestion.getSpecificName()));
                allocatedTable.addCell(new Paragraph(percentageFormat.format(suggestion.getAnnualRate().multiply(new BigDecimal("100")))).setTextAlignment(TextAlignment.RIGHT));
                allocatedTable.addCell(new Paragraph("₹" + currencyFormat.format(suggestion.getMonthlySipRequiredForGoal())).setTextAlignment(TextAlignment.RIGHT));
            }
            document.add(allocatedTable);
        }

        document.close();
        return baos.toByteArray();
    }

    /**
     * Helper method to format a BigDecimal amount into a currency string (e.g., 1,234.56).
     * @param amount The BigDecimal amount to format.
     * @return Formatted currency string.
     */
    private String formatCurrency(BigDecimal amount) {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        return df.format(amount);
    }
}