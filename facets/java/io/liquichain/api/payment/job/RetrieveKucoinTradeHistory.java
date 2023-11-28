package io.liquichain.api.payment.job;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.meveo.admin.exception.BusinessException;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.service.script.Script;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.utils.Numeric;

public class RetrieveKucoinTradeHistory extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(RetrieveKucoinTradeHistory.class);
    private static final String BASE_URL = "https://api.kucoin.com/api/v2";
    private static final String ENDPOINT = "/market/histories?symbol=KLUB";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ParamBeanFactory paramBeanFactory = getCDIBean(ParamBeanFactory.class);
    private final ParamBean config = paramBeanFactory.getInstance();

    private final String apiKey = config.getProperty("kucoin.api.key", "");
    private final String apiSecret = config.getProperty("kucoin.api.secret", "");
    private final String apiPassphrase = config.getProperty("kucoin.api.passphrase", "");

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        try {
            String url = BASE_URL + ENDPOINT;

            ResteasyClient client = new ResteasyClientBuilder().build();
            WebTarget webTarget = client.target(url);
            String now = String.valueOf(Instant.now().toEpochMilli());

            Invocation.Builder invocationBuilder = webTarget
                    .request(MediaType.APPLICATION_JSON)
                    .header("Content-Type", "application/json")
                    .header("KC-API-KEY", apiKey)
                    .header("KC-API-SIGN", generateSignature(now))
                    .header("KC-API-TIMESTAMP", now)
                    .header("KC-API-PASSPHRASE", apiPassphrase);

            Response response = invocationBuilder.get();

            if (response.getStatus() == 200) {
                String responseData = response.readEntity(String.class);
                LOG.info("Received response from kucoin: {}", toJson(responseData));
                Map<String, Object> responseMap = convert(responseData);
            } else {
                throw new RuntimeException("Data retrieval failed. HTTP error code: {}" + response.getStatus());
            }

        } catch (Exception e) {
            throw new BusinessException("Failed to retrieve trade history from kucoin.", e);
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

    public static String toJson(Object data) {
        String json = null;
        try {
            json = OBJECT_MAPPER.writeValueAsString(data);
        } catch (Exception e) {
            LOG.error("Failed to convert to json: {}", data, e);
        }
        return json;
    }

    public static <T> T convert(String data) {
        T value = null;
        try {
            value = OBJECT_MAPPER.readValue(data, new TypeReference<T>() {
            });
        } catch (Exception e) {
            LOG.error("Failed to parse data: {}", data, e);
        }
        return value;
    }

}