package io.liquichain.api.payment.job;

import static java.math.RoundingMode.HALF_UP;
import static org.meveo.commons.utils.StringUtils.isBlank;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.model.customEntities.TradeHistory;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.service.storage.RepositoryService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.utils.Numeric;

public class RetrieveKucoinTradeHistory extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(RetrieveKucoinTradeHistory.class);
    private static final String BASE_URL = "https://api.kucoin.com/api/v1";
    private static final String EXCHANGE_RATE_URL = "http://api.exchangerate.host/live?currencies=EUR&access_key=";
    private static final String ENDPOINT = "/market/histories?symbol=KLUB-USDT";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final KlubToUSD klubToUSD = KlubToUSD.getInstance();
    private static final ExchangeRate exchangeRate = ExchangeRate.getInstance();

    private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private final Repository defaultRepo = repositoryService.findDefaultRepository();
    private final ParamBeanFactory paramBeanFactory = getCDIBean(ParamBeanFactory.class);
    private final ParamBean config = paramBeanFactory.getInstance();

    private final String apiKey = config.getProperty("kucoin.api.key", "");
    private final String apiSecret = config.getProperty("kucoin.api.secret", "");
    private final String apiPassphrase = config.getProperty("kucoin.api.passphrase", "");
    private final String exchangeRateKey = config.getProperty("exchangerate.api.key", "");

    public BigDecimal retrieveKlubToUSDRate() {
        String rate = klubToUSD.getRate();
        if (isBlank(rate)) {
            return parseDecimal("0.015");
        }
        return parseDecimal(klubToUSD.getRate());
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        try {
            String url = BASE_URL + ENDPOINT;

            ResteasyClient client = new ResteasyClientBuilder().build();
            WebTarget webTarget = client.target(url);
            String now = String.valueOf(Instant.now().toEpochMilli());

            Response response = webTarget
                    .request(MediaType.APPLICATION_JSON)
                    .header("Content-Type", "application/json")
                    .header("KC-API-KEY", apiKey)
                    .header("KC-API-SIGN", generateSignature(now))
                    .header("KC-API-TIMESTAMP", now)
                    .header("KC-API-PASSPHRASE", apiPassphrase)
                    .get();

            if (response.getStatus() == 200) {
                String responseData = response.readEntity(String.class);
                LOG.debug("Received response from kucoin: {}", responseData);
                saveData(responseData);
            } else {
                throw new RuntimeException("Data retrieval failed. HTTP error code: {}" + response.getStatus());
            }

            updateCachedRate();

        } catch (Exception e) {
            throw new BusinessException("Failed to retrieve trade history from kucoin.", e);
        }
    }

    private void updateCachedRate() {
        try {
            TradeHistory latestHistory = crossStorageApi.find(defaultRepo, TradeHistory.class)
                                                        .orderBy("time", false)
                                                        .limit(1)
                                                        .getResult();
            klubToUSD.updateRate(latestHistory.getPrice());
        } catch (Exception e) {
            LOG.error("Failed to retrieve latest trade history.", e);
        }
    }

    private void saveData(String responseData) {
        if (isBlank(responseData)) {
            throw new RuntimeException("Response from kucoin was empty.");
        }

        Map<String, Object> responseMap = convert(responseData);

        if (responseMap == null) {
            throw new RuntimeException("Response map was null.");
        }

        String statusCode = "" + responseMap.get("code");
        if (!"200000".equals(statusCode)) {
            throw new RuntimeException("Failed to retrieve kucoin trade history. Status code: " + statusCode);
        }

        List<Map<String, Object>> tradeHistoryList = convert(responseMap.get("data"));

        if (tradeHistoryList == null) {
            throw new RuntimeException("Trade history list data was empty.");
        }

        BigDecimal exchangeRate = retrieveExchangeRate();
        BigDecimal eurToUsd = exchangeRate == null ? parseDecimal("1.1") : exchangeRate;

        tradeHistoryList.sort(Comparator.comparing(item -> String.valueOf(item.get("sequence"))));

        BigDecimal previousPrice = BigDecimal.ZERO;
        for (Map<String, Object> item : tradeHistoryList) {
            String sequence = "" + item.get("sequence");
            String price = "" + item.get("price");
            BigDecimal currentPrice = parseDecimal(price);
            String priceEuro = String.valueOf(parseInverse(currentPrice.multiply(eurToUsd).setScale(9, HALF_UP)));
            String size = "" + item.get("size");
            String side = "" + item.get("side");
            Instant time = Instant.ofEpochMilli(((Long) item.get("time")) / 1000000);

            Double percentChange = currentPrice.subtract(previousPrice)
                                               .divide(previousPrice, 9, HALF_UP)
                                               .multiply(parseDecimal("100"))
                                               .doubleValue();

            TradeHistory tradeHistory = new TradeHistory();
            tradeHistory.setUuid(sequence);
            tradeHistory.setPrice(price);
            tradeHistory.setPriceEuro(priceEuro);
            tradeHistory.setPercentChange(percentChange);
            tradeHistory.setSize(size);
            tradeHistory.setSide(side);
            tradeHistory.setTime(time);

            try {
                crossStorageApi.createOrUpdate(defaultRepo, tradeHistory);
            } catch (Exception e) {
                LOG.error("Failed to save trade history: {}", toJson(item), e);
            }
            previousPrice = parseDecimal(price);
        }
    }

    public BigDecimal retrieveExchangeRate() {
        try {
            long now = Instant.now().toEpochMilli();
            if(exchangeRate.getRate() == null || (now > exchangeRate.getNextRateUpdate())) {
                ResteasyClient client = new ResteasyClientBuilder().build();
                WebTarget webTarget = client.target(EXCHANGE_RATE_URL + exchangeRateKey);
                Response response = webTarget.request(MediaType.APPLICATION_JSON).get(Response.class);

                if (response == null || response.getStatus() != 200) {
                    return null;
                }
                String responseData = response.readEntity(String.class);
                LOG.info("Exchange rate response: {}", responseData);

                Map<String, Object> responseMap = convert(responseData);
                if (responseMap == null) {
                    throw new RuntimeException("Failed to map response data.");
                }
                Boolean success = convert(responseMap.get("success"));
                if (!Boolean.TRUE.equals(success)) {
                    throw new RuntimeException("Response status: " + success);
                }
                Map<String, Double> rates = convert(responseMap.get("quotes"));
                if (rates == null) {
                    throw new RuntimeException("Rates received was empty");
                }
                exchangeRate.setRate(parseDecimal(rates.get("USDEUR")));
                exchangeRate.setNextRateUpdate();
            }
            return exchangeRate.getRate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve exchange rate.", e);
        }
    }

    private String generateSignature(String timestamp) {
        try {
            String message = timestamp + "GET" + ENDPOINT;
            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
            byte[] keyBytes = apiSecret.getBytes(StandardCharsets.UTF_8);

            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, HMAC_ALGORITHM);
            mac.init(secretKeySpec);

            byte[] signatureBytes = mac.doFinal(messageBytes);

            return Numeric.toHexStringNoPrefix(signatureBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate signature.", e);
        }
    }

    private String toJson(Object data) {
        try {
            return OBJECT_MAPPER.writeValueAsString(data);
        } catch (Exception e) {
            LOG.error("Failed to convert to json: {}", data, e);
        }
        return null;
    }

    private <T> T convert(String data) {
        try {
            return OBJECT_MAPPER.readValue(data, new TypeReference<T>() {
            });
        } catch (Exception e) {
            LOG.error("Failed to parse data: {}", data, e);
        }
        return null;
    }

    private <T> T convert(Object data) {
        try {
            return OBJECT_MAPPER.convertValue(data, new TypeReference<T>() {
            });
        } catch (Exception e) {
            LOG.error("Failed to parse data: {}", data, e);
        }
        return null;
    }

    public BigDecimal parseDecimal(String price) {
        return new BigDecimal(price).setScale(9, HALF_UP);
    }

    public BigDecimal parseDecimal(Double price) {
        return new BigDecimal(price).setScale(9, HALF_UP);
    }

    public BigDecimal parseInverse(String price) {
        return BigDecimal.ONE.divide(parseDecimal(price), 9, HALF_UP);
    }

    public BigDecimal parseInverse(BigDecimal price) {
        return BigDecimal.ONE.divide(price, 9, HALF_UP);
    }

}

class KlubToUSD {
    private String rate;

    private static class KlubToUSDHolder {
        private static final KlubToUSD INSTANCE = new KlubToUSD();
    }

    public static KlubToUSD getInstance() {
        return KlubToUSDHolder.INSTANCE;
    }

    public void updateRate(String rate) {
        this.rate = rate;
    }

    public String getRate() {
        return rate;
    }
}

class ExchangeRate {
    private BigDecimal rate;
    private long nextRateUpdate = 0;

    private static class ExchangeRateHolder {
        private static final ExchangeRate INSTANCE = new ExchangeRate();
    }

    public static ExchangeRate getInstance() {
        return ExchangeRateHolder.INSTANCE;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public long getNextRateUpdate() {
        return nextRateUpdate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }
    public void setNextRateUpdate(){
        // replace this with the applicable rate update frequency as described in https://exchangerate.host/product
        // free plan only has daily updates
        this.nextRateUpdate = Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli();
    }
}