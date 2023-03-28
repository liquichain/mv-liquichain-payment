package io.liquichain.api.payment;

import static io.liquichain.api.payment.ConversionRateScript.CONVERSION_RATE;
import static java.math.RoundingMode.HALF_EVEN;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.api.rest.technicalservice.EndpointScript;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.customEntities.PaypalOrder;
import org.meveo.model.customEntities.Wallet;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.service.storage.RepositoryService;

import io.liquichain.api.core.LiquichainTransaction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.paypal.core.PayPalEnvironment;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.http.exceptions.HttpException;
import com.paypal.orders.*;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LiquichainPaymentScript extends EndpointScript {
    private static final Logger LOG = LoggerFactory.getLogger(LiquichainPaymentScript.class);

    private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private final Repository defaultRepo = repositoryService.findDefaultRepository();
    private final ParamBeanFactory paramBeanFactory = getCDIBean(ParamBeanFactory.class);
    private final ParamBean config = paramBeanFactory.getInstance();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Gson gson = new Gson();

    private final String PAYMENT_WALLET = config
        .getProperty("payment.wallet", "b4bF880BAfaF68eC8B5ea83FaA394f5133BB9623");
    public final String ORIGIN_WALLET = PAYMENT_WALLET.toLowerCase();
    private final String RETURN_URL = config.getProperty("payment.capture.url", "https://dev.jips.io/");

    private final LiquichainTransaction liquichainTransaction = new LiquichainTransaction();
    private String result;
    private String orderId = null;

    public String getResult() {
        return result;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        Map<String, Object> fromAmount = (Map<String, Object>) parameters.get("from");
        Map<String, Object> toAmount = (Map<String, Object>) parameters.get("to");
        String toWalletId = normalizeHash("" + parameters.get("toWallet"));
        String fromWalletId = normalizeHash("" + parameters.get("fromWallet"));
        String path = this.endpointRequest.getPathInfo();
        if (path.lastIndexOf("/") == 16) {
            orderId = path.substring(17);
        }

        LOG.info("parsing orderId from path, path={}  index={}", path, path.lastIndexOf("/"));
        LOG.info("execute paymentScript, orderId={}  toWallet=0x{}", orderId, toWalletId);

        Order order;
        OrderService orderService = new OrderService();

        if (orderId == null || "0".equals(orderId)) {
            LOG.info("Creating new payment order");

            if (fromAmount == null) {
                String errorMessage = "The from parameter must include currency and amount.";
                LOG.error(errorMessage);
                result = createErrorResponse(null, "400", errorMessage);
                return;
            }

            if (toAmount == null) {
                String errorMessage = "The to parameter must include currency and amount.";
                LOG.error(errorMessage);
                result = createErrorResponse(null, "400", errorMessage);
                return;
            }

            String fromCurrency = "" + fromAmount.get("currency");
            String fromValue = "" + fromAmount.get("amount");
            String toCurrency = "" + toAmount.get("currency");
            String toValue = "" + toAmount.get("amount");

            BigDecimal conversionRate = CONVERSION_RATE.get(fromCurrency + "_TO_" + toCurrency);
            BigDecimal decimalFromValue = new BigDecimal(fromValue).setScale(18, HALF_EVEN);
            BigDecimal convertedFromValue = conversionRate.multiply(decimalFromValue).setScale(18, HALF_EVEN);
            BigDecimal parsedToValue = new BigDecimal(toValue).setScale(18, HALF_EVEN);

            boolean sameAmount = convertedFromValue.equals(parsedToValue);

            LOG.info("Comparing amounts from: {}, to: {}, equal: {}", convertedFromValue, parsedToValue, sameAmount);
            BigDecimal billedValue = decimalFromValue.setScale(2, HALF_EVEN);
            LOG.info("Billed value: {}", billedValue);

            if (!sameAmount) {
                String errorMessage = "The from(" + fromValue + fromCurrency + "=" + convertedFromValue + toCurrency +
                    ") and to(" + parsedToValue + ") amounts are not the same.";
                LOG.error(errorMessage);
                result = createErrorResponse(null, "400", errorMessage);
                return;
            }

            Wallet toWallet;
            try {
                toWallet = crossStorageApi.find(defaultRepo, toWalletId, Wallet.class);
                if (toWallet == null) {
                    String errorMessage = "Destination wallet does not exist, walletId=" + toWalletId;
                    LOG.error(errorMessage);
                    result = createErrorResponse(null, "404", errorMessage);
                    return;
                }
            } catch (Exception e) {
                String errorMessage = "Recipient wallet not found, walletId=" + toWalletId;
                LOG.error(errorMessage, e);
                result = createErrorResponse(null, "404", errorMessage);
                return;
            }

            boolean hasFromWallet = false;
            Wallet fromWallet = null;
            try {
                fromWallet = crossStorageApi.find(defaultRepo, fromWalletId, Wallet.class);
                hasFromWallet = fromWallet != null;
            } catch (Exception e) {
                LOG.warn("Sender wallet not found, walletId={}", fromWalletId);
                // do nothing, from wallet is optional
            }

            try {
                Wallet originWallet = crossStorageApi.find(defaultRepo, ORIGIN_WALLET, Wallet.class);
                BigInteger amount = new BigDecimal(toValue).movePointRight(18).toBigInteger();
                BigInteger originBalance = new BigInteger(originWallet.getBalance());
                LOG.info("origin: 0x{}, old balance: {}, amount: {}", ORIGIN_WALLET, originWallet.getBalance(), amount);
                if (amount.compareTo(originBalance) <= 0) {
                    LOG.info("create paypal order");
                    OrderItem orderItem = new OrderItem();
                    orderItem.setCurrencyCode(fromCurrency);
                    orderItem.setValue(billedValue.toString());
                    List<OrderItem> orderItems = new ArrayList<>();
                    orderItems.add(orderItem);
                    order = orderService.createOrder(orderItems, RETURN_URL);
                    LOG.info("return orderId: {}", order.id());
                    PaypalOrder paypalOrder = new PaypalOrder();
                    paypalOrder.setCreationDate(Instant.now());
                    paypalOrder.setOrderId(order.id());
                    paypalOrder.setFromWallet(hasFromWallet ? fromWallet.getUuid() : ORIGIN_WALLET);
                    paypalOrder.setFromCurrency(fromCurrency);
                    paypalOrder.setFromAmount(fromValue);
                    paypalOrder.setToWallet(toWallet.getUuid());
                    paypalOrder.setToCurrency(toCurrency);
                    paypalOrder.setToAmount(toValue);
                    paypalOrder.setStatus("CREATED");
                    paypalOrder.setUuid(crossStorageApi.createOrUpdate(defaultRepo, paypalOrder));
                    result = gson.toJson(order);
                    LOG.info("Paypal order created: {}", result);
                } else {
                    LOG.error("Insufficient global balance");
                    result = createErrorResponse(null, "501", "Insufficient global balance");
                }
            } catch (Exception e) {
                LOG.error("Error creating order", e);
                result = createErrorResponse(null, "-32700", e.getMessage());
            }
        } else {
            LOG.info("Capturing payment order: {}", orderId);
            order = orderService.captureOrder(orderId);
            PaypalOrder paypalOrder = crossStorageApi.find(defaultRepo, PaypalOrder.class)
                                                     .by("orderId", orderId)
                                                     .getResult();

            if (order == null || paypalOrder == null) {
                String errorMessage = "Cannot capture order:" + orderId;
                if (paypalOrder != null) {
                    paypalOrder.setStatus("KO");
                    paypalOrder.setError(errorMessage);
                    try {
                        crossStorageApi.createOrUpdate(defaultRepo, paypalOrder);
                    } catch (Exception e) {
                        LOG.error("Failed to update paypalOrder", e);
                    }
                }
                result = createErrorResponse(orderId, "406", errorMessage);
            } else if (StringUtils.isNotBlank(order.status()) && "COMPLETED".equalsIgnoreCase(order.status())) {
                try {
                    BigInteger amountInDemos = (new BigDecimal(paypalOrder.getToAmount())).multiply(
                        BigDecimal.TEN.pow(18)).toBigInteger();
                    String transactionHash = liquichainTransaction
                        .transferSmartContract(ORIGIN_WALLET, paypalOrder.getToWallet(),
                            amountInDemos, "topup", "Paypal topup",
                            "You received your paypal topup!", paypalOrder.getFromWallet());
                    LOG.info("created transaction, transactionHash={}", transactionHash);
                    paypalOrder.setStatus("OK");
                    try {
                        crossStorageApi.createOrUpdate(defaultRepo, paypalOrder);
                    } catch (Exception e) {
                        LOG.error("Failed to update paypalOrder", e);
                    }
                    result = "Success";
                    result = createResponse(paypalOrder);
                } catch (Exception e) {
                    LOG.error("Paypal ok but transaction ko: {}", order);
                    String errorMessage = "Transaction error:" + e.getMessage();
                    paypalOrder.setStatus("ALERT");
                    paypalOrder.setError(errorMessage);
                    try {
                        crossStorageApi.createOrUpdate(defaultRepo, paypalOrder);
                    } catch (Exception ex) {
                        LOG.error("Failed to update paypalOrder", e);
                    }
                    result = createErrorResponse(orderId, "406", errorMessage);
                }

            } else {
                String errorMessage = "Capture failed:" + order.status();
                paypalOrder.setStatus("KO");
                paypalOrder.setError(errorMessage);
                try {
                    crossStorageApi.createOrUpdate(defaultRepo, paypalOrder);
                } catch (Exception e) {
                    LOG.error("Failed to update paypalOrder", e);
                }
                result = createErrorResponse(orderId, "406", errorMessage);
            }
        }
    }

    private static String normalizeHash(String hash) {
        if (hash == null) {
            return null;
        }
        if (hash.startsWith("0x")) {
            return hash.substring(2).toLowerCase();
        }
        return hash.toLowerCase();
    }

    private String createResponse(PaypalOrder order) {
        String orderJson;
        Map<String, Object> orderMap = new LinkedHashMap<>();
        if (order != null) {
            orderMap.put("uuid", order.getUuid());
            orderMap.put("orderId", order.getOrderId());
            orderMap.put("fromWallet", order.getFromWallet());
            orderMap.put("fromCurrency", order.getFromCurrency());
            orderMap.put("fromAmount", order.getFromAmount());
            orderMap.put("toWallet", order.getToWallet());
            orderMap.put("toCurrency", order.getToCurrency());
            orderMap.put("toAmount", order.getToAmount());
            orderMap.put("status", order.getStatus());
            orderMap.put("error", order.getError());
        }
        try {
            orderJson = mapper.writeValueAsString(orderMap);
        } catch (JsonProcessingException e) {
            // used error code from https://github.com/claudijo/json-rpc-error
            return createErrorResponse(orderId, "-32700", e.getMessage());
        }

        String response = "{\n" +
            "  \"status\": \"success\",\n" +
            "  \"result\": " + orderJson + "\n" +
            "}";
        LOG.debug("response: {}", response);
        return response;
    }

    private String createErrorResponse(String orderId, String errorCode, String message) {
        String idFormat = orderId == null || NumberUtils.isParsable(orderId)
            ? "  \"id\": %s,"
            : "  \"id\": \"%s\",";
        String response = "{\n" +
            String.format(idFormat, orderId) + "\n" +
            "  \"status\": \"" + errorCode + "\",\n" +
            "  \"message\": \"" + message + "\",\n" +
            "}";
        LOG.debug("error response: {}", response);
        return response;
    }
}


class OrderItem {
    private String currencyCode;
    private String value;

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}


class OrderService extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(OrderService.class);
    private final ParamBeanFactory paramBeanFactory = getCDIBean(ParamBeanFactory.class);
    private final ParamBean config = paramBeanFactory.getInstance();

    private final String PAYMENT_MODE = config.getProperty("payment.mode", "PRODUCTION");
    private final String SANDBOX_CLIENT_ID = config
        .getProperty("payment.sandbox.client.id",
            "AWVIDI2xMJE0AXDOdEtttOW0WgrLzeNWBUAKClN4bVYXdeP2Hkx3BXPlXOahZs0palbyhcpzrow9ZMg3");
    private final String SANDBOX_CLIENT_SECRET = config
        .getProperty("payment.sandbox.client.secret",
            "EOHmSfHQQxACD94zzZOBqPXy3ETALxOTdpr-KRLw4ECRs0Bk3olEhn9AQTz922J6o3U5L47Se5x727l_");
    private final String LIVE_CLIENT_ID = config
        .getProperty("payment.live.client.id",
            "AWVIDI2xMJE0AXDOdEtttOW0WgrLzeNWBUAKClN4bVYXdeP2Hkx3BXPlXOahZs0palbyhcpzrow9ZMg3");
    private final String LIVE_CLIENT_SECRET = config
        .getProperty("payment.live.client.secret",
            "EOHmSfHQQxACD94zzZOBqPXy3ETALxOTdpr-KRLw4ECRs0Bk3olEhn9AQTz922J6o3U5L47Se5x727l_");

    private PayPalEnvironment environment = null;

    private PayPalHttpClient client = null;
    private boolean debug = false;

    public OrderService() {
        boolean isDevMode = "DEVELOPMENT".equals(PAYMENT_MODE);
        this.debug = isDevMode;
        if (isDevMode) {
            environment = new PayPalEnvironment.Sandbox(SANDBOX_CLIENT_ID, SANDBOX_CLIENT_SECRET);
        } else {
            environment = new PayPalEnvironment.Live(LIVE_CLIENT_ID, LIVE_CLIENT_SECRET);
        }
        this.client = new PayPalHttpClient(environment);
    }

    public Order createOrder(List<OrderItem> orderItems, String RETURN_URL) {
        Order order = null;
        boolean hasOrderItems = orderItems != null && !orderItems.isEmpty();
        if (hasOrderItems) {
            OrderRequest orderRequest = new OrderRequest();
            orderRequest.checkoutPaymentIntent("CAPTURE");

            List<PurchaseUnitRequest> purchaseUnits = new ArrayList<>();

            orderItems.forEach((item) -> purchaseUnits
                .add(new PurchaseUnitRequest()
                    .amountWithBreakdown(
                        new AmountWithBreakdown()
                            .currencyCode(item.getCurrencyCode())
                            .value(item.getValue()))));
            orderRequest.purchaseUnits(purchaseUnits);

            ApplicationContext appContext = new ApplicationContext();
            appContext.returnUrl(RETURN_URL);
            appContext.cancelUrl(RETURN_URL);
            orderRequest.applicationContext(appContext);

            OrdersCreateRequest request = new OrdersCreateRequest().requestBody(orderRequest);

            try {
                HttpResponse<Order> response = this.client.execute(request);

                order = response.result();

                if (this.debug) {
                    LOG.debug("Order ID: " + order.id());
                    order.links()
                         .forEach(link -> LOG.debug(
                             link.rel() + " => " + link.method() + ":" + link.href()));
                }

            } catch (IOException ioe) {
                String errorMessage;
                if (ioe instanceof HttpException) {
                    errorMessage = "Failed to create order.";
                } else {
                    errorMessage = "Unknown error while creating order.";
                }
                throw new RuntimeException(errorMessage, ioe);
            }
        } else {
            LOG.warn("No order details provided, will not process creation of order.");
        }
        return order;
    }

    public Order captureOrder(String orderId) {
        Order order = null;
        OrdersCaptureRequest request = new OrdersCaptureRequest(orderId);

        try {
            HttpResponse<Order> response = this.client.execute(request);
            order = response.result();

            if (this.debug) {
                LOG.debug("Status Code: " + response.statusCode());
                LOG.debug("Status: " + order.status());
                LOG.debug("Order ID: " + order.id());
                LOG.debug("Links: ");
                for (LinkDescription link : order.links()) {
                    LOG.debug("\t" + link.rel() + ": " + link.href());
                }
                LOG.debug("Capture ids:");
                for (PurchaseUnit purchaseUnit : order.purchaseUnits()) {
                    for (Capture capture : purchaseUnit.payments().captures()) {
                        LOG.debug("\t" + capture.id());
                    }
                }
            }

        } catch (IOException ioe) {
            if (ioe instanceof HttpException) {
                LOG.error("Failed to capture order.", ioe);
            } else {
                LOG.error("Unknown error while capturing order.", ioe);
            }
        }
        return order;
    }
}
