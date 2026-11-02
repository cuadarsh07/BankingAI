package in.ashish.combined.dto.fraud;

import in.ashish.combined.entity.fraud.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRiskDto {
    private RiskLevel riskLevel;
    private double statisticalScore;
    private BigDecimal userAvgAmount;
    private BigDecimal userMaxAmount;
    private long historicalTxnCount;
    private boolean isFirstTimeRecipient;
    private boolean isRapidSuccessiveTxn;
    private boolean isUnusualTiming;
    private double amountMultiplierOfAvg;
    private List<String> riskFactors;
}

