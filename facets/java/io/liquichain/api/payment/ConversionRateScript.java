package io.liquichain.api.payment;

import static java.math.RoundingMode.HALF_UP;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
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

    public BigDecimal EUR_TO_USD = parseDecimal("1.1");

    public BigDecimal LCN_TO_EUR = parseDecimal("2000");
    public BigDecimal EUR_TO_LCN = parseInverse(LCN_TO_EUR);

    public BigDecimal CFA_TO_EUR = new BigDecimal("0.015").setScale(24, HALF_UP);
    public BigDecimal EUR_TO_CFA = BigDecimal.ONE.divide(CFA_TO_EUR, 24, HALF_UP);

    public final Map<String, BigDecimal> CONVERSION_RATE = new HashMap<>() {{
        put("LCN_TO_EUR", LCN_TO_EUR);
        put("EUR_TO_LCN", EUR_TO_LCN);
        put("CFA_TO_EUR", CFA_TO_EUR);
        put("EUR_TO_CFA", EUR_TO_CFA);
    }};

    private Map<String, Object> result;

    public Map<String, Object> getResult() {
        return result;
    }

    private void setRates() {
        BigDecimal KLUB_TO_USD = parseDecimal("0.015");
        BigDecimal KLUB_TO_EUR = EUR_TO_USD.multiply(KLUB_TO_USD).setScale(9, HALF_UP);
        BigDecimal klubToUSD = tradeHistory.retrieveKlubToUSDRate();
        if (klubToUSD != null) {
            KLUB_TO_USD = klubToUSD;
        }
        CONVERSION_RATE.put("KLUB_TO_USD", KLUB_TO_USD);
        CONVERSION_RATE.put("USD_TO_KLUB", parseInverse(KLUB_TO_USD));

        BigDecimal klubToEUR = tradeHistory.retrieveKlubToEurRate();
        if (klubToEUR != null) {
            KLUB_TO_EUR = klubToEUR;
        }
        CONVERSION_RATE.put("KLUB_TO_EUR", KLUB_TO_EUR);
        CONVERSION_RATE.put("EUR_TO_KLUB", parseInverse(KLUB_TO_EUR));
    }

    {
        setRates();
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        downloadRates();
        result = new HashMap<>() {{
            put("timestamp", Instant.now().toEpochMilli());
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

    private void downloadRates() {
        try {
            EUR_TO_USD = tradeHistory.retrieveExchangeRate();
            LOG.info("EUR to USD exchange rate: {}", EUR_TO_USD);
            setRates();
        } catch (Exception e) {
            LOG.warn("Failed to retrieve exchange rate: {}", e.getMessage());
        }
    }

    private BigDecimal parseDecimal(String price) {
        return new BigDecimal(price).setScale(9, HALF_UP);
    }

    private BigDecimal parseInverse(BigDecimal price) {
        return BigDecimal.ONE.divide(price, 9, HALF_UP);
    }
}
