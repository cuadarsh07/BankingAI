package in.ashish.combined.service.fraud;

import in.ashish.combined.dto.fraud.TransactionRiskDto;
import in.ashish.combined.entity.Account;
import in.ashish.combined.entity.Transaction;
import in.ashish.combined.entity.fraud.RiskLevel;
import in.ashish.combined.entity.fraud.TransactionRisk;
import in.ashish.combined.repository.TransactionRepository;
import in.ashish.combined.repository.fraud.TransactionRiskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for deterministic Java statistical anomaly detection and risk scoring.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionRiskService {

    private final TransactionRepository transactionRepository;
    private final TransactionRiskRepository transactionRiskRepository;

    private static final BigDecimal FALLBACK_NEW_USER_MED = new BigDecimal("15000.00");
    private static final BigDecimal FALLBACK_NEW_USER_HIGH = new BigDecimal("50000.00");
    private static final BigDecimal FALLBACK_NEW_USER_CRIT = new BigDecimal("200000.00");

    /**
     * Evaluates statistical transaction anomaly signals and returns a TransactionRiskDto.
     */
    @Transactional(readOnly = true)
    public TransactionRiskDto evaluateRisk(Account sourceAccount, Account destinationAccount, BigDecimal amount) {
        Long sourceAccountId = sourceAccount.getId();
        Long destAccountId = destinationAccount != null ? destinationAccount.getId() : null;

        // Fetch recent outgoing transfers for statistical calculation (up to last 100)
        List<Transaction> historical = transactionRepository.findHistoricalTransfers(sourceAccountId, PageRequest.of(0, 100));
        long historicalCount = historical.size();

        List<String> riskFactors = new ArrayList<>();
        double score = 10.0; // Baseline low risk

        // Check 1: First-time recipient
        boolean isFirstTimeRecipient = true;
        if (destAccountId != null && historicalCount > 0) {
            isFirstTimeRecipient = !transactionRepository.hasTransferredToCounterpartyBefore(sourceAccountId, destAccountId);
            if (isFirstTimeRecipient) {
                score += 20.0;
                riskFactors.add("FIRST_TIME_RECIPIENT: Sender has never transferred to this account before");
            }
        }

        // Check 2: Unusual timing (e.g. 1:00 AM to 5:00 AM)
        LocalTime nowTime = LocalTime.now();
        boolean isUnusualTiming = (nowTime.isAfter(LocalTime.of(1, 0)) && nowTime.isBefore(LocalTime.of(5, 0)));
        if (isUnusualTiming) {
            score += 15.0;
            riskFactors.add("UNUSUAL_TIMING: Transfer initiated during off-hours (01:00 - 05:00)");
        }

        // Check 3: Rapid successive transactions (3+ transfers in past 10 minutes)
        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
        long recentTxnCount = transactionRepository.countRecentTransfers(sourceAccountId, tenMinutesAgo);
        boolean isRapidSuccessive = recentTxnCount >= 3;
        if (isRapidSuccessive) {
            score += 30.0;
            riskFactors.add(String.format("RAPID_TRANSFERS: %d outgoing transfers in the last 10 minutes", recentTxnCount));
        }

        // Check 4: Safe Fallback for New Accounts / Insufficient History (< 3 transactions)
        BigDecimal userAvg = BigDecimal.ZERO;
        BigDecimal userMax = BigDecimal.ZERO;
        double amountMultiplier = 1.0;

        if (historicalCount < 3) {
            riskFactors.add("NEW_USER_FALLBACK: Account has fewer than 3 historical transactions; applying baseline limits");
            if (amount.compareTo(FALLBACK_NEW_USER_CRIT) > 0) {
                score = Math.max(score, 85.0);
                riskFactors.add(String.format("HIGH_AMOUNT_NEW_USER: Transfer amount ₹%s exceeds ₹%s critical ceiling", amount, FALLBACK_NEW_USER_CRIT));
            } else if (amount.compareTo(FALLBACK_NEW_USER_HIGH) > 0) {
                score = Math.max(score, 65.0);
                riskFactors.add(String.format("SUBSTANTIAL_AMOUNT_NEW_USER: Transfer amount ₹%s exceeds ₹%s high-risk ceiling", amount, FALLBACK_NEW_USER_HIGH));
            } else if (amount.compareTo(FALLBACK_NEW_USER_MED) > 0) {
                score = Math.max(score, 35.0);
            }
        } else {
            // Calculate historical statistics
            BigDecimal sum = BigDecimal.ZERO;
            for (Transaction t : historical) {
                sum = sum.add(t.getAmount());
                if (t.getAmount().compareTo(userMax) > 0) {
                    userMax = t.getAmount();
                }
            }
            userAvg = sum.divide(BigDecimal.valueOf(historicalCount), 2, RoundingMode.HALF_UP);

            if (userAvg.compareTo(BigDecimal.ZERO) > 0) {
                amountMultiplier = amount.divide(userAvg, 2, RoundingMode.HALF_UP).doubleValue();
            }

            // Check 5: Sudden Large Transfer compared to User's Average
            if (amountMultiplier >= 10.0 && amount.compareTo(new BigDecimal("50000.00")) >= 0) {
                score += 45.0;
                riskFactors.add(String.format("SUDDEN_LARGE_TRANSFER: Amount ₹%s is %.1fx greater than user average (₹%s)",
                        amount, amountMultiplier, userAvg));
            } else if (amountMultiplier >= 5.0 && amount.compareTo(new BigDecimal("25000.00")) >= 0) {
                score += 25.0;
                riskFactors.add(String.format("ELEVATED_AMOUNT: Amount ₹%s is %.1fx greater than user average (₹%s)",
                        amount, amountMultiplier, userAvg));
            } else if (amountMultiplier >= 3.0) {
                score += 15.0;
                riskFactors.add(String.format("ABOVE_AVERAGE: Amount is %.1fx user's normal average", amountMultiplier));
            }

            // Check 6: Exceeds Historical Maximum
            if (amount.compareTo(userMax) > 0 && amount.compareTo(new BigDecimal("100000.00")) >= 0) {
                score += 25.0;
                riskFactors.add(String.format("EXCEEDS_HISTORICAL_MAX: Amount ₹%s exceeds prior maximum of ₹%s", amount, userMax));
            }

            // Check 7: Combined First-Time Recipient + Sudden Large Spike
            if (isFirstTimeRecipient && amountMultiplier >= 5.0) {
                score += 20.0;
                riskFactors.add("CRITICAL_COMBINATION: Sudden massive spike sent to unverified first-time recipient");
            }
        }

        // Clamp score between 0 and 100
        score = Math.min(100.0, Math.max(0.0, score));

        // Map score to RiskLevel
        RiskLevel riskLevel;
        if (score >= 80.0) {
            riskLevel = RiskLevel.CRITICAL;
        } else if (score >= 55.0) {
            riskLevel = RiskLevel.HIGH;
        } else if (score >= 25.0) {
            riskLevel = RiskLevel.MEDIUM;
        } else {
            riskLevel = RiskLevel.LOW;
        }

        return TransactionRiskDto.builder()
                .riskLevel(riskLevel)
                .statisticalScore(score)
                .userAvgAmount(userAvg)
                .userMaxAmount(userMax)
                .historicalTxnCount(historicalCount)
                .isFirstTimeRecipient(isFirstTimeRecipient)
                .isRapidSuccessiveTxn(isRapidSuccessive)
                .isUnusualTiming(isUnusualTiming)
                .amountMultiplierOfAvg(amountMultiplier)
                .riskFactors(riskFactors)
                .build();
    }

    /**
     * Persists transaction risk audit record.
     */
    @Transactional
    public TransactionRisk recordRisk(String transactionReference, Account sourceAccount, Account destinationAccount,
                                      BigDecimal amount, TransactionRiskDto dto) {
        TransactionRisk risk = TransactionRisk.builder()
                .transactionReference(transactionReference)
                .sourceAccount(sourceAccount)
                .destinationAccount(destinationAccount)
                .amount(amount)
                .riskLevel(dto.getRiskLevel())
                .statisticalScore(dto.getStatisticalScore())
                .userAvgAmount(dto.getUserAvgAmount())
                .userMaxAmount(dto.getUserMaxAmount())
                .historicalTxnCount(dto.getHistoricalTxnCount())
                .isFirstTimeRecipient(dto.isFirstTimeRecipient())
                .isRapidSuccessiveTxn(dto.isRapidSuccessiveTxn())
                .isUnusualTiming(dto.isUnusualTiming())
                .amountMultiplierOfAvg(dto.getAmountMultiplierOfAvg())
                .riskFactors(String.join("; ", dto.getRiskFactors()))
                .build();

        return transactionRiskRepository.save(risk);
    }
}

