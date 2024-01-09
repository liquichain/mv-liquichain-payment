package io.liquichain.api.payment;

import static java.math.RoundingMode.HALF_UP;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.meveo.admin.exception.BusinessException;
import org.meveo.service.script.Script;

import io.liquichain.api.payment.job.RetrieveKucoinTradeHistory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConversionRateScript extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(ConversionRateScript.class);
    private static final long RATE_EXCHANGE_QUERY_DELAY_IN_MS = 10000;

    private final RetrieveKucoinTradeHistory tradeHistory = new RetrieveKucoinTradeHistory();

    public BigDecimal LCN_TO_EUR = parseDecimal("2000");
    public BigDecimal EUR_TO_LCN = parseInverse(LCN_TO_EUR);

    public BigDecimal CFA_TO_EUR = new BigDecimal("0.015").setScale(24, HALF_UP);
    public BigDecimal EUR_TO_CFA = BigDecimal.ONE.divide(CFA_TO_EUR, 24, HALF_UP);

    public BigDecimal getConversionRate(String rateKey) {

        LOG.info("getConversionRate: {}", rateKey);
        BigDecimal rate = CONVERSION_RATE.get(rateKey);
        if (rate == null) {
            updateRates();
            CONVERSION_RATE.forEach((key, value) -> LOG.info("rate: {} = {}", key, value));
            rate = CONVERSION_RATE.get(rateKey);
            LOG.info("rate: {} = {}", rateKey, rate);
        }
        return rate;
    }

    public final Map<String, BigDecimal> CONVERSION_RATE = new HashMap<>() {{
        put("LCN_TO_EUR", LCN_TO_EUR);
        put("EUR_TO_LCN", EUR_TO_LCN);
        put("CFA_TO_EUR", CFA_TO_EUR);
        put("EUR_TO_CFA", EUR_TO_CFA);
    }};

    public final List<String> FROM_TRADE_HISTORY = List.of("KLUB_TO_USD", "USD_TO_KLUB", "KLUB_TO_EUR", "EUR_TO_KLUB");

    private Map<String, Object> result;

    public Map<String, Object> getResult() {
        return result;
    }

    private String updateRates() {
        Map<String, String> conversionRateMap = tradeHistory.retrieveConversionRateMap();
        CONVERSION_RATE.putAll(extractConversionRates(conversionRateMap));
        return conversionRateMap.get("sequenceId");
    }

    public Map<String, BigDecimal> extractConversionRates(Map<String, String> conversionRateMap) {
        BigDecimal klubToUsdRate = parseDecimal("0.015");
        Map<String, BigDecimal> conversionRates = new HashMap<>();
        BigDecimal klubToUSD = parseDecimal(conversionRateMap.get("usdRate"));
        if (klubToUSD != null) {
            klubToUsdRate = klubToUSD;
        }
        conversionRates.put("KLUB_TO_USD", klubToUsdRate);
        conversionRates.put("USD_TO_KLUB", parseInverse(klubToUsdRate));

        BigDecimal klubToEurRate = parseDecimal("1.1").multiply(klubToUsdRate).setScale(9, HALF_UP);
        BigDecimal klubToEUR = parseDecimal(conversionRateMap.get("eurRate"));
        if (klubToEUR != null) {
            klubToEurRate = klubToEUR;
        }
        conversionRates.put("KLUB_TO_EUR", klubToEurRate);
        conversionRates.put("EUR_TO_KLUB", parseInverse(klubToEurRate));
        return conversionRates;
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        String sequenceId = updateRates();
        result = new HashMap<>() {{
            put("timestamp", Instant.now().toEpochMilli());
            put("sequenceId", sequenceId);
            put("data", CONVERSION_RATE
                    .entrySet().stream()
                    .map(entry -> {
                        String[] symbols = entry.getKey().split("_TO_");
                        return Map.of(
                                "from", Map.of("value", 1, "currency", symbols[0]),
                                "to", Map.of("value", entry.getValue(), "currency", symbols[1])
                        );
                    })
                    .collect(Collectors.toList()));
        }};
    }

    private BigDecimal parseDecimal(String price) {
        return new BigDecimal(price).setScale(9, HALF_UP);
    }

    private BigDecimal parseInverse(BigDecimal price) {
        return BigDecimal.ONE.divide(price, 9, HALF_UP);
    }
}
