package io.liquichain.api.payment;

import static java.math.RoundingMode.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;

public class ConversionRateScript extends Script {

    private String result;

    public String getResult() {
        return result;
    }

    public static BigDecimal ONE = new BigDecimal(1);
    public static BigDecimal LCN_TO_EUR = new BigDecimal("2000").setScale(9, HALF_UP);
    public static BigDecimal EUR_TO_LCN = ONE.divide(LCN_TO_EUR, 9, HALF_UP);
    public static BigDecimal KLUB_TO_EUR = new BigDecimal("1000").setScale(9, HALF_UP);
    public static BigDecimal EUR_TO_KLUB = ONE.divide(KLUB_TO_EUR, 9, HALF_UP);
    public static BigDecimal CFA_TO_EUR = new BigDecimal("0.0015").setScale(9, HALF_UP);
    public static BigDecimal EUR_TO_CFA = new BigDecimal("655.13").setScale(9, HALF_UP);
    public static BigDecimal KLUB_TO_USD = new BigDecimal("0.015").setScale(9, HALF_UP);
    public static BigDecimal USD_TO_KLUB = ONE.divide(KLUB_TO_USD, 9, HALF_UP);

    public static final Map<String, BigDecimal> CONVERSION_RATE = new HashMap<>();

    static {
        CONVERSION_RATE.put("LCN_TO_EUR", LCN_TO_EUR);
        CONVERSION_RATE.put("EUR_TO_LCN", EUR_TO_LCN);
        CONVERSION_RATE.put("KLUB_TO_EUR", KLUB_TO_EUR);
        CONVERSION_RATE.put("EUR_TO_KLUB", EUR_TO_KLUB);
        CONVERSION_RATE.put("EUR_TO_CFA", EUR_TO_CFA);
        CONVERSION_RATE.put("CFA_TO_EUR", CFA_TO_EUR);
        CONVERSION_RATE.put("KLUB_TO_USD", KLUB_TO_USD);
        CONVERSION_RATE.put("USD_TO_KLUB", USD_TO_KLUB);
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {

        result = "{\"data\":[\n"
            + "{\"from\":{\"value\":1,\"currency\":\"LCN\"},\"to\":{\"value\":" + LCN_TO_EUR + ",\"currency\":\"EUR\"}},\n"
            + "{\"from\":{\"value\":1,\"currency\":\"EUR\"},\"to\":{\"value\":" + EUR_TO_LCN + ",\"currency\":\"LCN\"}},\n"
            + "{\"from\":{\"value\":1,\"currency\":\"KLUB\"},\"to\":{\"value\":" + KLUB_TO_EUR + ",\"currency\":\"EUR\"}},\n"
            + "{\"from\":{\"value\":1,\"currency\":\"EUR\"},\"to\":{\"value\":" + EUR_TO_KLUB + ",\"currency\":\"KLUB\"}},\n"
            + "{\"from\":{\"value\":1,\"currency\":\"EUR\"},\"to\":{\"value\":" + EUR_TO_CFA + ",\"currency\":\"CFA\"}},\n"
            + "{\"from\":{\"value\":1,\"currency\":\"CFA\"},\"to\":{\"value\":" + CFA_TO_EUR + ",\"currency\":\"EUR\"}},\n"
            + "{\"from\":{\"value\":1,\"currency\":\"KLUB\"},\"to\":{\"value\":" + KLUB_TO_USD + ",\"currency\":\"USD\"}},\n"
            + "{\"from\":{\"value\":1,\"currency\":\"USD\"},\"to\":{\"value\":" + USD_TO_KLUB + ",\"currency\":\"KLUB\"}}\n"
            + "],\n"
            + "\"timestamp\":" + System.currentTimeMillis() + "\n"
            + "}";
    }
}
