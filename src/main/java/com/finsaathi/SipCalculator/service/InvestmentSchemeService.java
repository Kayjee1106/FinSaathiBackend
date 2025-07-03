package com.finsaathi.SipCalculator.service;

import com.finsaathi.SipCalculator.model.InvestmentSchemeRate;
import com.finsaathi.SipCalculator.model.SchemeSipSuggestion;
import com.finsaathi.SipCalculator.repository.InvestmentSchemeRateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator; // NEW IMPORT
import java.util.List;
import java.util.Map; // NEW IMPORT
import java.util.Optional;
import java.util.stream.Collectors; // NEW IMPORT
import java.util.logging.Logger;

@Service
public class InvestmentSchemeService {

    private static final Logger logger = Logger.getLogger(InvestmentSchemeService.class.getName());

    @Autowired
    private InvestmentSchemeRateRepository investmentSchemeRateRepository;

    @Transactional
    public InvestmentSchemeRate saveSchemeRate(InvestmentSchemeRate schemeRate) {
        Optional<InvestmentSchemeRate> existing = investmentSchemeRateRepository
                .findBySpecificNameAndYear(schemeRate.getSpecificName(), schemeRate.getYear());
        if (existing.isPresent()) {
            logger.info("Scheme rate already exists for " + schemeRate.getSpecificName() + " for year " + schemeRate.getYear() + ". Skipping insert/update.");
            return existing.get();
        }
        return investmentSchemeRateRepository.save(schemeRate);
    }

    public List<InvestmentSchemeRate> getAllSchemes() {
        return investmentSchemeRateRepository.findAll();
    }

    public List<InvestmentSchemeRate> getSchemeRatesByYear(Integer year) {
        return investmentSchemeRateRepository.findByYear(year);
    }

    /**
     * NEW METHOD: Calculates the allocated monthly SIP for the best scheme in each category
     * based on the overall optimal monthly SIP and the 50-30-20 principle.
     *
     * @param overallMonthlySipRequired The single, optimal monthly SIP amount from the main calculation.
     * @param timePeriodYears The time horizon in years.
     * @return A list of SchemeSipSuggestion DTOs (expected to be 3 entries: best Bank, best MF, best Eq/Gold).
     */
    public List<SchemeSipSuggestion> getOptimalAllocatedSchemeSuggestions(BigDecimal overallMonthlySipRequired, Integer timePeriodYears) {
        List<SchemeSipSuggestion> suggestions = new ArrayList<>();

        if (overallMonthlySipRequired == null || overallMonthlySipRequired.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warning("Overall monthly SIP is invalid or zero. Cannot provide allocated suggestions.");
            return suggestions;
        }
        if (timePeriodYears == null) {
            logger.warning("Time period is null. Cannot provide allocated suggestions.");
            return suggestions;
        }

        // 1. Fetch all schemes for the given time period
        List<InvestmentSchemeRate> allSchemesForYear = investmentSchemeRateRepository.findByYear(timePeriodYears);

        // Group schemes by product type
        Map<String, List<InvestmentSchemeRate>> schemesByType = allSchemesForYear.stream()
                .collect(Collectors.groupingBy(InvestmentSchemeRate::getProductType));

        // Define allocation percentages
        BigDecimal bankAllocation = new BigDecimal("0.50"); // 50%
        BigDecimal mfAllocation = new BigDecimal("0.30");   // 30%
        BigDecimal equityGoldAllocation = new BigDecimal("0.20"); // 20%

        // Determine the best scheme for each category and calculate allocated SIP
        // --- Best Bank Savings Scheme (50% allocation) ---
        Optional<InvestmentSchemeRate> bestBankScheme = schemesByType.getOrDefault("Bank Savings Products", new ArrayList<>()).stream()
                .max(Comparator.comparing(InvestmentSchemeRate::getAnnualRate));

        bestBankScheme.ifPresent(scheme -> {
            BigDecimal allocatedSip = overallMonthlySipRequired.multiply(bankAllocation).setScale(2, RoundingMode.HALF_UP);
            suggestions.add(new SchemeSipSuggestion(
                    scheme.getProductType(),
                    scheme.getSpecificName(),
                    scheme.getDescription(),
                    scheme.getYear(),
                    scheme.getAnnualRate(),
                    allocatedSip
            ));
        });

        // --- Best Mutual Fund Scheme (30% allocation) ---
        Optional<InvestmentSchemeRate> bestMfScheme = schemesByType.getOrDefault("Mutual funds", new ArrayList<>()).stream()
                .max(Comparator.comparing(InvestmentSchemeRate::getAnnualRate));

        bestMfScheme.ifPresent(scheme -> {
            BigDecimal allocatedSip = overallMonthlySipRequired.multiply(mfAllocation).setScale(2, RoundingMode.HALF_UP);
            suggestions.add(new SchemeSipSuggestion(
                    scheme.getProductType(),
                    scheme.getSpecificName(),
                    scheme.getDescription(),
                    scheme.getYear(),
                    scheme.getAnnualRate(),
                    allocatedSip
            ));
        });

        // --- Best Equity/Gold Scheme (20% allocation) ---
        Optional<InvestmentSchemeRate> bestEquityScheme = schemesByType.getOrDefault("Equity", new ArrayList<>()).stream()
                .max(Comparator.comparing(InvestmentSchemeRate::getAnnualRate));

        Optional<InvestmentSchemeRate> bestGoldScheme = schemesByType.getOrDefault("Gold", new ArrayList<>()).stream()
                .max(Comparator.comparing(InvestmentSchemeRate::getAnnualRate));

        Optional<InvestmentSchemeRate> bestEquityGoldScheme;
        if (bestEquityScheme.isPresent() && bestGoldScheme.isPresent()) {
            bestEquityGoldScheme = bestEquityScheme.get().getAnnualRate().compareTo(bestGoldScheme.get().getAnnualRate()) > 0
                    ? bestEquityScheme : bestGoldScheme;
        } else if (bestEquityScheme.isPresent()) {
            bestEquityGoldScheme = bestEquityScheme;
        } else if (bestGoldScheme.isPresent()) {
            bestEquityGoldScheme = bestGoldScheme;
        } else {
            bestEquityGoldScheme = Optional.empty();
        }

        bestEquityGoldScheme.ifPresent(scheme -> {
            BigDecimal allocatedSip = overallMonthlySipRequired.multiply(equityGoldAllocation).setScale(2, RoundingMode.HALF_UP);
            suggestions.add(new SchemeSipSuggestion(
                    scheme.getProductType(),
                    scheme.getSpecificName(),
                    scheme.getDescription(),
                    scheme.getYear(),
                    scheme.getAnnualRate(),
                    allocatedSip
            ));
        });

        // Sort the suggestions (e.g., by Product Type for consistent display)
        suggestions.sort(Comparator.comparing(SchemeSipSuggestion::getProductType));

        return suggestions;
    }


    /** Helper method to pre-populate initial investment scheme data. (Same as previous version) */
    @Transactional
    public void populateInitialSchemeData() {
        logger.info("Populating initial investment scheme data with specific fund names...");
        createAndSaveScheme("Equity", "Stocks | Top 25 Sensex Stocks", "Top 25 companies in stock Market", 1, new BigDecimal("0.02"));
        createAndSaveScheme("Equity", "Stocks | Top 25 Sensex Stocks", null, 2, new BigDecimal("0.10"));
        createAndSaveScheme("Equity", "Stocks | Top 25 Sensex Stocks", null, 3, new BigDecimal("0.21"));
        createAndSaveScheme("Equity", "Stocks | Top 25 Sensex Stocks", null, 4, new BigDecimal("0.24"));
        createAndSaveScheme("Equity", "Stocks | Top 25 Sensex Stocks", null, 5, new BigDecimal("0.30"));
        createAndSaveScheme("Gold", "Digital Gold", "Gold investment in digital form", 1, new BigDecimal("0.08"));
        createAndSaveScheme("Gold", "Digital Gold", null, 2, new BigDecimal("0.21"));
        createAndSaveScheme("Gold", "Digital Gold", null, 3, new BigDecimal("0.17"));
        createAndSaveScheme("Gold", "Digital Gold", null, 4, new BigDecimal("0.17"));
        createAndSaveScheme("Gold", "Digital Gold", null, 5, new BigDecimal("0.24"));
        createAndSaveScheme("Mutual funds", "UTI Nifty Next 50 Index Fund Direct - Growth", null, 1, new BigDecimal("0.08"));
        createAndSaveScheme("Mutual funds", "DSP Nifty Next 50 Index Fund Direct - Growth", null, 2, new BigDecimal("0.21"));
        createAndSaveScheme("Mutual funds", "ICICI Prudential Nifty Next 50 Index Fund Direct - Growth", null, 3, new BigDecimal("0.17"));
        createAndSaveScheme("Mutual funds", "SBI Nifty Index Direct Plan-Growth", null, 4, new BigDecimal("0.19"));
        createAndSaveScheme("Mutual funds", "HDFC Nifty 50 Index Fund Direct-Growth", null, 5, new BigDecimal("0.24"));
        createAndSaveScheme("Mutual funds", "Nippon India Large Cap Fund Direct-Growth", null, 1, new BigDecimal("0.108"));
        createAndSaveScheme("Mutual funds", "Nippon India Large Cap Fund Direct-Growth", null, 2, new BigDecimal("0.21"));
        createAndSaveScheme("Mutual funds", "ICICI Prudential Bluechip Fund Direct-Growth", null, 3, new BigDecimal("0.19"));
        createAndSaveScheme("Mutual funds", "ICICI Prudential Bluechip Fund Direct-Growth", null, 4, new BigDecimal("0.19"));
        createAndSaveScheme("Mutual funds", "HDFC Large Cap Fund Direct-Growth", null, 5, new BigDecimal("0.25"));
        createAndSaveScheme("Mutual funds", "HDFC Midcap Opportunities Fund Direct-Growth", null, 1, new BigDecimal("0.097"));
        createAndSaveScheme("Mutual funds", "Nippon India Growth Fund Direct-Growth", null, 2, new BigDecimal("0.30"));
        createAndSaveScheme("Mutual funds", "ICICI Prudential Midcap Direct Plan-Growth", null, 3, new BigDecimal("0.25"));
        createAndSaveScheme("Mutual funds", "SBI Magnum Mid Cap Direct Plan-Growth", null, 4, new BigDecimal("0.28"));
        createAndSaveScheme("Mutual funds", "Kotak Emerging Equity Fund Direct-Growth", null, 5, new BigDecimal("0.33"));
        createAndSaveScheme("Mutual funds", "Tata Small Cap Fund Direct - Growth", null, 1, new BigDecimal("0.07"));
        createAndSaveScheme("Mutual funds", "Nippon India Small Cap Fund Direct-Growth", null, 2, new BigDecimal("0.26"));
        createAndSaveScheme("Mutual funds", "Franklin India Smaller Companies Fund Direct-Growth", null, 3, new BigDecimal("0.23"));
        createAndSaveScheme("Mutual funds", "HDFC Small Cap Fund Direct-Growth", null, 4, new BigDecimal("0.23"));
        createAndSaveScheme("Mutual funds", "Axis Small Cap Fund Direct-Growth", null, 5, new BigDecimal("0.38"));
        createAndSaveScheme("Mutual funds", "ICICI Prudential Equity & Debt Fund Direct-Growth", null, 1, new BigDecimal("0.11"));
        createAndSaveScheme("Mutual funds", "DSP Aggressive Hybrid Fund Direct-Growth", null, 2, new BigDecimal("0.21"));
        createAndSaveScheme("Mutual funds", "Kotak Equity Hybrid Fund Direct-Growth", null, 3, new BigDecimal("0.18"));
        createAndSaveScheme("Mutual funds", "UTI Aggressive Hybrid Fund Direct Fund-Growth", null, 4, new BigDecimal("0.19"));
        createAndSaveScheme("Mutual funds", "SBI Equity Hybrid Fund Direct Plan-Growth", null, 5, new BigDecimal("0.23"));
        createAndSaveScheme("Mutual funds", "HDFC Multi Asset Fund Direct-Growth", null, 1, new BigDecimal("0.10"));
        createAndSaveScheme("Mutual funds", "Tata Multi Asset Opportunities Fund Direct - Growth", null, 2, new BigDecimal("0.19"));
        createAndSaveScheme("Mutual funds", "SBI Multi Asset Allocation Fund Direct-Growth", null, 3, new BigDecimal("0.17"));
        createAndSaveScheme("Mutual funds", "Axis Multi Asset Allocation Direct Plan-Growth", null, 4, new BigDecimal("0.18"));
        createAndSaveScheme("Mutual funds", "ICICI Prudential Multi Asset Fund Direct-Growth", null, 5, new BigDecimal("0.22"));
        createAndSaveScheme("Mutual funds", "ICICI Prudential Short Term Debt Fund Direct Plan-Growth", null, 1, new BigDecimal("0.09"));
        createAndSaveScheme("Mutual funds", "HDFC Short Term Debt Fund Direct Plan-Growth", null, 2, new BigDecimal("0.085"));
        createAndSaveScheme("Mutual funds", "SBI Short Term Debt Fund Direct Plan-Growth", null, 3, new BigDecimal("0.08"));
        createAndSaveScheme("Mutual funds", "Tata Short Term Bond Direct Plan-Growth", null, 4, new BigDecimal("0.08"));
        createAndSaveScheme("Mutual funds", "Nippon India Short Term Fund Direct-Growth", null, 5, new BigDecimal("0.071"));
        createAndSaveScheme("Bank Savings Products", "SBI Recurring Deposit", null, 1, new BigDecimal("0.068"));
        createAndSaveScheme("Bank Savings Products", "HDFC Bank Recurring Deposit", null, 2, new BigDecimal("0.067"));
        createAndSaveScheme("Bank Savings Products", "Kotak Recurring Deposit", null, 3, new BigDecimal("0.067"));
        createAndSaveScheme("Bank Savings Products", "ICICI Bank Recurring Deposit", null, 4, new BigDecimal("0.067"));
        createAndSaveScheme("Bank Savings Products", "Axis Bank Recurring Deposit", null, 5, new BigDecimal("0.067"));
        createAndSaveScheme("Bank Savings Products", "Axis Bank Fixed Deposit", null, 1, new BigDecimal("0.03"));
        createAndSaveScheme("Bank Savings Products", "Kotak Fixed Deposit", null, 2, new BigDecimal("0.06"));
        createAndSaveScheme("Bank Savings Products", "SBI Fixed Deposit", null, 3, new BigDecimal("0.07"));
        createAndSaveScheme("Bank Savings Products", "HDFC Bank Fixed Deposit", null, 4, new BigDecimal("0.07"));
        createAndSaveScheme("Bank Savings Products", "Bandhan Fixed Deposit", null, 5, new BigDecimal("0.07"));
        logger.info("Initial investment scheme data population complete with specific fund names.");
    }

    private void createAndSaveScheme(String productType, String specificName, String description, Integer year, BigDecimal annualRate) {
        InvestmentSchemeRate schemeRate = new InvestmentSchemeRate();
        schemeRate.setProductType(productType);
        schemeRate.setSpecificName(specificName);
        schemeRate.setDescription(description);
        schemeRate.setYear(year);
        schemeRate.setAnnualRate(annualRate);
        saveSchemeRate(schemeRate);
    }
}