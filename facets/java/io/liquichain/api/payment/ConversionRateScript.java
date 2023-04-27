package io.liquichain.api.payment;

import static java.math.RoundingMode.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;

import org.json.*;

import javax.ws.rs.client.*;
import javax.ws.rs.core.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConversionRateScript extends Script {

    private static final Logger log = LoggerFactory.getLogger(ConversionRateScript.class);

    private String result;

    public String getResult() {
        return result;
    }

    public static BigDecimal LCN_TO_EUR = new BigDecimal("2000").setScale(9, HALF_UP);
    public static BigDecimal EUR_TO_LCN = BigDecimal.ONE.divide(LCN_TO_EUR, 9, HALF_UP);
    public static BigDecimal KLUB_TO_USD = new BigDecimal("0.015").setScale(9, HALF_UP);
    public static BigDecimal USD_TO_KLUB = BigDecimal.ONE.divide(KLUB_TO_USD, 9, HALF_UP);
    public static BigDecimal EUR_TO_USD = new BigDecimal("1.1").setScale(9, HALF_UP);
    public static BigDecimal CFA_TO_EUR = new BigDecimal("0.015").setScale(24, HALF_UP);
    public static BigDecimal EUR_TO_CFA = BigDecimal.ONE.divide(CFA_TO_EUR, 24, HALF_UP);

    public static final Map<String, BigDecimal> CONVERSION_RATE = new HashMap<>();

    static void setRates() {
        CONVERSION_RATE.put("LCN_TO_EUR", LCN_TO_EUR);
        CONVERSION_RATE.put("EUR_TO_LCN", EUR_TO_LCN);
        CONVERSION_RATE.put("KLUB_TO_USD", KLUB_TO_USD);
        CONVERSION_RATE.put("USD_TO_KLUB", USD_TO_KLUB);
        BigDecimal EUR_TO_KLUB = USD_TO_KLUB.multiply(EUR_TO_USD).setScale(9, HALF_UP);
        BigDecimal KLUB_TO_EUR = BigDecimal.ONE.divide(EUR_TO_KLUB, 9, HALF_UP);
        CONVERSION_RATE.put("KLUB_TO_EUR", KLUB_TO_EUR);
        CONVERSION_RATE.put("EUR_TO_KLUB", EUR_TO_KLUB);
        CONVERSION_RATE.put("CFA_TO_EUR", CFA_TO_EUR);
        CONVERSION_RATE.put("EUR_TO_CFA", EUR_TO_CFA);
    }

    static {
        setRates();
    }

    static long nextRateUpdate = 0;
    static long rateExchangeQueryDelayInMs = 10000;

    void downloadRates() {
        if (System.currentTimeMillis() < nextRateUpdate) {
            return;
        }
        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.target("https://api.exchangerate.host/latest?symbols=EUR,USD");
        Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);
        Response response = null;
        double EURtoUSD = 1.1;
        try {
            response = webTarget.request().get(Response.class);
            log.debug("Exchange rate response: {}", response);
            String value = response.readEntity(String.class);
            JSONObject json = new JSONObject(value);
            if (json.getBoolean("success")) {
                double rate = json.getJSONObject("rates").getDouble("USD");
                log.info("New EUR/USD rate : {}", rate);
                EUR_TO_USD = new BigDecimal(rate).setScale(9, HALF_UP);
            }
            setRates();
            nextRateUpdate = System.currentTimeMillis() + rateExchangeQueryDelayInMs;
        } catch (Exception ex) {
            log.warn("error while getting exchange rate :{}", ex.getMessage());
        }
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        downloadRates();
        result = "{\"data\":[\n"
            + "{\"from\":{\"value\":1,\"currency\":\"LCN\"},\"to\":{\"value\":" + CONVERSION_RATE.get("LCN_TO_EUR") +
            ",\"currency\":\"EUR\"}},\n"
            + "{\"from\":{\"value\":1,\"currency\":\"EUR\"},\"to\":{\"value\":" + CONVERSION_RATE.get("EUR_TO_LCN") +
            ",\"currency\":\"LCN\"}},\n"
            + "{\"from\":{\"value\":1,\"currency\":\"KLUB\"},\"to\":{\"value\":" + CONVERSION_RATE.get("KLUB_TO_EUR") +
            ",\"currency\":\"EUR\"}},\n"
            + "{\"from\":{\"value\":1,\"currency\":\"EUR\"},\"to\":{\"value\":" + CONVERSION_RATE.get("EUR_TO_KLUB") +
            ",\"currency\":\"KLUB\"}},\n"
            + "{\"from\":{\"value\":1,\"currency\":\"KLUB\"},\"to\":{\"value\":" + CONVERSION_RATE.get("KLUB_TO_USD") +
            ",\"currency\":\"USD\"}},\n"
            + "{\"from\":{\"value\":1,\"currency\":\"USD\"},\"to\":{\"value\":" + CONVERSION_RATE.get("USD_TO_KLUB") +
            ",\"currency\":\"KLUB\"}},\n"
            + "{\"from\":{\"value\":1,\"currency\":\"EUR\"},\"to\":{\"value\":" + CONVERSION_RATE.get("EUR_TO_CFA") +
            ",\"currency\":\"CFA\"}},\n"
            + "{\"from\":{\"value\":1,\"currency\":\"CFA\"},\"to\":{\"value\":" + CONVERSION_RATE.get("CFA_TO_EUR") +
            ",\"currency\":\"EUR\"}}\n"
            + "],\n"
            + "\"timestamp\":" + System.currentTimeMillis() + "\n"
            + "}";
    }
}
