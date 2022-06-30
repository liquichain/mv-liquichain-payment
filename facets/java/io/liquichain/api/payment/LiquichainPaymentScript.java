package io.liquichain.api.payment;

import com.paypal.core.PayPalEnvironment;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.http.exceptions.HttpException;
import com.paypal.orders.*;

import java.util.*;
import java.time.Instant;
import java.math.BigInteger;
import java.math.BigDecimal;
import java.io.IOException;

import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.commons.utils.StringUtils;
import org.meveo.api.rest.technicalservice.EndpointScript;
import org.meveo.admin.exception.BusinessException;
import org.meveo.service.script.Script;
import org.meveo.model.customEntities.Wallet;
import org.meveo.model.customEntities.PaypalOrder;
import org.meveo.model.storage.Repository;
import org.meveo.service.storage.RepositoryService;
import org.meveo.api.persistence.CrossStorageApi;

import io.liquichain.api.core.LiquichainTransaction;

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

public class LiquichainPaymentScript extends EndpointScript {
    private static final Logger LOG = LoggerFactory.getLogger(LiquichainPaymentScript.class);

    private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private final Repository defaultRepo = repositoryService.findDefaultRepository();
    private final ParamBeanFactory paramBeanFactory = getCDIBean(ParamBeanFactory.class);
    private final ParamBean config = paramBeanFactory.getInstance();
    private final ObjectMapper mapper =  new ObjectMapper();

    public final String ORIGIN_WALLET = "b4bF880BAfaF68eC8B5ea83FaA394f5133BB9623".toLowerCase();
    // config.getProperty("wallet.origin.account", "deE0d5bE78E1Db0B36d3C1F908f4165537217333");
    private final String RETURN_URL = config
            .getProperty("payment.capture.url", "https://dev.telecelplay.io/");

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
        String message = "Wallet does not exist";
        Map<String, Object> from = (Map<String, Object>) parameters.get("from");
        Map<String, Object> to = (Map<String, Object>) parameters.get("to");
        String publicAddress = (String) parameters.get("account");
        String path = this.endpointRequest.getPathInfo();
        if (path.lastIndexOf("/") == 16) {
            orderId = path.substring(17);
        }
        LOG.info("execute paymentScript, orderId={}  account=0x{}, path={}, lastIndex:{}",
                orderId, publicAddress, path, path.lastIndexOf("/"));
        List<OrderItem> orderItems = new ArrayList<>();

        if (from != null) {
            OrderItem orderItem = new OrderItem();
            orderItem.setCurrencyCode("" + from.get("currency"));
            orderItem.setValue("" + from.get("amount"));
            orderItems.add(orderItem);
        }

        Order order = null;
        OrderService orderService = new OrderService();

        if (orderId == null || "0".equals(orderId)) {
            if (StringUtils.isNotBlank(publicAddress)) {
                try {
                    Wallet toWallet = crossStorageApi.find(defaultRepo, publicAddress.toLowerCase(),
                            Wallet.class);
                    Wallet fromWallet =
                            crossStorageApi.find(defaultRepo, ORIGIN_WALLET, Wallet.class);
                    BigInteger amount = new BigDecimal(to.get("amount").toString())
                            .movePointRight(18).toBigInteger();
                    BigInteger originBalance = new BigInteger(fromWallet.getBalance());
                    LOG.info("origin wallet: 0x{} old balance:{} amount:{}",
                            ORIGIN_WALLET, fromWallet.getBalance(), amount);
                    if (amount.compareTo(originBalance) <= 0) {
                        LOG.info("create paypal order");
                        order = orderService.createOrder(orderItems, RETURN_URL);
                        LOG.info("return orderId :{}", order.id());
                        PaypalOrder paypalOrder = new PaypalOrder();
                        paypalOrder.setCreationDate(Instant.now());
                        paypalOrder.setOrderId(order.id());
                        paypalOrder.setFromWallet(ORIGIN_WALLET);
                        paypalOrder.setFromCurrency(from.get("currency").toString());
                        paypalOrder.setFromAmount(from.get("amount").toString());
                        paypalOrder.setToWallet(publicAddress.toLowerCase());
                        // FIXME; use conversion rate
                        paypalOrder.setToCurrency(to.get("currency").toString());
                        paypalOrder.setToAmount(to.get("amount").toString());
                        paypalOrder.setStatus("CREATED");
                        crossStorageApi.createOrUpdate(defaultRepo, paypalOrder);
                        result = mapper.writeValueAsString(order);
                        LOG.info("persisted paypalOrder, result order:{}", result);
                    } else {
                        LOG.error("Insufficient global balance");
                        result = createErrorResponse(null, "501", "Insufficient global balance");
                    }
                } catch (Exception e) {
                    LOG.error("Error creating order", e);
                    result = createErrorResponse(null, "-32700", e.getMessage());
                }
            } else {
                LOG.error("account publicAddress:{}", publicAddress);
                result = createErrorResponse(null, "404", message);
            }
        } else {
            LOG.info("capture {}", orderId);
            order = orderService.captureOrder(orderId);
            PaypalOrder paypalOrder = crossStorageApi.find(defaultRepo, PaypalOrder.class)
                    .by("orderId", orderId)
                    .getResult();

            if (order == null || paypalOrder == null) {
                message = "Cannot capture order:" + orderId;
                if (paypalOrder != null) {
                    paypalOrder.setStatus("KO");
                    paypalOrder.setError(message);
                    try {
                        crossStorageApi.createOrUpdate(defaultRepo, paypalOrder);
                    } catch (Exception e) {
                        LOG.error("Failed to update paypalOrder", e);
                    }
                }
                result = createErrorResponse(orderId, "406", message);
            } else if (StringUtils.isNotBlank(order.status())
                    && "COMPLETED".equalsIgnoreCase(order.status())) {
                try {
                    BigInteger amountInDemos = (new BigDecimal(paypalOrder.getToAmount())).multiply(
                            BigDecimal.TEN.pow(18)).toBigInteger();
                    String transactionHash = liquichainTransaction
                            .transferSmartContract(ORIGIN_WALLET, paypalOrder.getToWallet(),
                                    amountInDemos, "topup", "Paypal topup",
                                    "You received your paypal topup!");
                    LOG.info("created transaction, transactionHash={}", transactionHash);
                    paypalOrder.setStatus("OK");
                    try {
                        crossStorageApi.createOrUpdate(defaultRepo, paypalOrder);
                    } catch (Exception e) {
                        LOG.error("Failed to update paypalOrder", e);
                    }
                    result = "Success";
                    result = createResponse(orderId, null);
                } catch (Exception e) {
                    LOG.error("Paypal ok but transaction ko: {}", order);
                    message = "Transaction error:" + e.getMessage();
                    paypalOrder.setStatus("ALERT");
                    paypalOrder.setError(message);
                    try {
                        crossStorageApi.createOrUpdate(defaultRepo, paypalOrder);
                    } catch (Exception ex) {
                        LOG.error("Failed to update paypalOrder", e);
                    }
                    result = createErrorResponse(orderId, "406", message);
                }

            } else {
                message = "Capture failed:" + order.status();
                paypalOrder.setStatus("KO");
                paypalOrder.setError(message);
                try {
                    crossStorageApi.createOrUpdate(defaultRepo, paypalOrder);
                } catch (Exception e) {
                    LOG.error("Failed to update paypalOrder", e);
                }
                result = createErrorResponse(orderId, "406", message);
            }
        }
    }

    public String createResponse(String orderId, Order order) {
        String idFormat = orderId == null || NumberUtils.isParsable(orderId)
                ? "  \"id\": %s,"
                : "  \"id\": \"%s\",";
        String orderJson = null;
        try {
            orderJson = mapper.writeValueAsString(order);
        } catch (JsonProcessingException e) {
            // used error code from https://github.com/claudijo/json-rpc-error
            return createErrorResponse(orderId, "-32700", e.getMessage());
        }

        String response = new StringBuilder()
                .append("{\n")
                .append(String.format(idFormat, orderId)).append("\n")
                .append("  \"status\": \"200\",\n")
                .append("  \"result\": ").append(orderJson).append("\n")
                .append("}").toString();
        LOG.debug("response: {}", response);
        return response;
    }

    public String createErrorResponse(String orderId, String errorCode, String message) {
        String idFormat = orderId == null || NumberUtils.isParsable(orderId)
                ? "  \"id\": %s,"
                : "  \"id\": \"%s\",";
        String response = new StringBuilder()
                .append("{\n")
                .append(String.format(idFormat, orderId)).append("\n")
                .append("  \"status\": \"").append(errorCode).append("\",\n")
                .append("  \"message\": \"").append(message).append("\",\n")
                .append("}").toString();
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

            orderItems.stream()
                    .forEach((item) -> purchaseUnits
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
                if (ioe instanceof HttpException) {
                    LOG.error("Failed to create order.", ioe);
                } else {
                    LOG.error("Unknown error while creating order.", ioe);
                }
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
