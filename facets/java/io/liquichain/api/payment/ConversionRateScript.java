package io.liquichain.api.payment;

import static io.liquichain.api.payment.job.RetrieveKucoinTradeHistory.*;
import static java.math.RoundingMode.HALF_UP;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.meveo.admin.exception.BusinessException;
import org.meveo.service.script.Script;

import io.liquichain.api.payment.job.RetrieveKucoinTradeHistory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConversionRateScript extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(ConversionRateScript.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final long RATE_EXCHANGE_QUERY_DELAY_IN_MS = 10000;

    public static BigDecimal EUR_TO_USD = parseDecimal("1.1");

    public static BigDecimal LCN_TO_EUR = parseDecimal("2000");
    public static BigDecimal EUR_TO_LCN = parseInverse(LCN_TO_EUR);

    public static BigDecimal KLUB_TO_USD = parseDecimal("0.015");
    public static BigDecimal USD_TO_KLUB = parseInverse(KLUB_TO_USD);

    public static BigDecimal CFA_TO_EUR = new BigDecimal("0.015").setScale(24, HALF_UP);
    public static BigDecimal EUR_TO_CFA = BigDecimal.ONE.divide(CFA_TO_EUR, 24, HALF_UP);

    private static long nextRateUpdate = 0;

    public static final Map<String, BigDecimal> CONVERSION_RATE = new HashMap<>() {{
        put("LCN_TO_EUR", LCN_TO_EUR);
        put("EUR_TO_LCN", EUR_TO_LCN);
        put("CFA_TO_EUR", CFA_TO_EUR);
        put("EUR_TO_CFA", EUR_TO_CFA);
    }};

    private Map<String, Object> result;

    public Map<String, Object> getResult() {
        return result;
    }

    static void setRates() {
        BigDecimal klubToUSD = RetrieveKucoinTradeHistory.retrieveKlubToUSDRate();
        if (klubToUSD != null) {
            KLUB_TO_USD = klubToUSD;
            USD_TO_KLUB = parseInverse(klubToUSD);
            CONVERSION_RATE.put("KLUB_TO_USD", KLUB_TO_USD);
            CONVERSION_RATE.put("USD_TO_KLUB", USD_TO_KLUB);
        }

        BigDecimal EUR_TO_KLUB = USD_TO_KLUB.multiply(EUR_TO_USD).setScale(9, HALF_UP);
        BigDecimal KLUB_TO_EUR = parseInverse(EUR_TO_KLUB);
        CONVERSION_RATE.put("KLUB_TO_EUR", KLUB_TO_EUR);
        CONVERSION_RATE.put("EUR_TO_KLUB", EUR_TO_KLUB);
    }

    static {
        setRates();
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        downloadRates();
        result = Map.of(
                "timestamp", Instant.now().toEpochMilli(),
                "data", CONVERSION_RATE
                        .entrySet().stream()
                        .map(entry -> {
                            String[] symbols = entry.getKey().split("_TO_");
                            return Map.of(
                                    "from", Map.of("value", 1, "currency", symbols[0]),
                                    "to", Map.of("value", entry.getValue(), "currency", symbols[1])
                            );
                        })
                        .collect(Collectors.toList()));
    }

    private void downloadRates() {
        if (System.currentTimeMillis() < nextRateUpdate) {
            return;
        }

        try {
            EUR_TO_USD = retrieveExchangeRate();
            setRates();
            nextRateUpdate = System.currentTimeMillis() + RATE_EXCHANGE_QUERY_DELAY_IN_MS;
        } catch (Exception e) {
            LOG.warn("Failed to retrieve exchange rate: {}", e.getMessage());
        }
    }

    public static String toJson(Object data) {
        try {
            return OBJECT_MAPPER.writeValueAsString(data);
        } catch (Exception e) {
            LOG.error("Failed to convert to json: {}", data, e);
        }
        return null;
    }
}
