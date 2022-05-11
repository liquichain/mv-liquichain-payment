package io.liquichain.api.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;

public class ConversionRateScript extends Script {

    private String result;

    public String getResult() {
        return result;
    }

    public static BigDecimal LCN_TO_EUR = (new BigDecimal("2000")).setScale(9, RoundingMode.HALF_UP);
    public static BigDecimal EUR_TO_LCN = new BigDecimal("0.0005").setScale(9, RoundingMode.HALF_UP);
    public static BigDecimal KLUB_TO_EUR = new BigDecimal("1000").setScale(9, RoundingMode.HALF_UP);
    public static BigDecimal EUR_TO_KLUB = new BigDecimal("0.001").setScale(9, RoundingMode.HALF_UP);
    public static BigDecimal EUR_TO_CFA = new BigDecimal("655.13").setScale(9, RoundingMode.HALF_UP);
    public static BigDecimal CFA_TO_EUR = new BigDecimal("0.0015").setScale(9, RoundingMode.HALF_UP);
    public static BigDecimal KLUB_TO_USD = new BigDecimal("1725.52").setScale(9, RoundingMode.HALF_UP);
    public static BigDecimal USD_TO_KLUB = new BigDecimal("0.0006").setScale(9, RoundingMode.HALF_UP);


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
