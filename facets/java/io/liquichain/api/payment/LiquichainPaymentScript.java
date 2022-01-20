package io.liquichain.api.payment;

import com.google.gson.Gson;
import io.liquichain.core.BlockForgerScript;

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

import org.apache.commons.collections.CollectionUtils;
import org.meveo.admin.util.pagination.PaginationConfiguration;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.customEntities.CustomEntityInstance;
import org.meveo.model.customEntities.CustomEntityTemplate;
import org.meveo.persistence.CrossStorageService;
import org.meveo.service.custom.CustomEntityTemplateService;
import org.meveo.service.custom.NativeCustomEntityInstanceService;
import org.meveo.api.rest.technicalservice.EndpointScript;
import org.meveo.admin.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigInteger;
import org.meveo.model.customEntities.Wallet;
import org.meveo.model.customEntities.PaypalOrder;
import org.meveo.model.customEntities.Transaction;
import org.meveo.model.storage.Repository;
import org.meveo.service.storage.RepositoryService;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.api.exception.EntityDoesNotExistsException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import org.web3j.crypto.*;

import javax.servlet.http.HttpServletRequest;

public class LiquichainPaymentScript extends EndpointScript {

    private static final Logger log = LoggerFactory.getLogger(LiquichainPaymentScript.class);

    private long chainId = 76;

    private String result;

    private String orderId = null;

    private String originWallet = "212dFDD1Eb4ee053b2f5910808B7F53e3D49AD2f".toLowerCase();

    private CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private Repository defaultRepo = repositoryService.findDefaultRepository();
    private CustomEntityTemplateService customEntityTemplateService = getCDIBean(CustomEntityTemplateService.class);
    private CrossStorageService crossStorageService = getCDIBean(CrossStorageService.class);

    public String getResult() {
        return result;
    }

    public void setOrderId(String orderId) {
      	log.info("orderId setter {}", orderId);
        this.orderId = orderId;
    }
  
  	public String getOrderId() {
    	return orderId;
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
      	log.info("orderId from path={}", getOrderId());
        String message = "Wallet does not exists";
        Map<String, Object> from = (Map<String, Object>) parameters.get("from");
        Map<String, Object> to = (Map<String, Object>) parameters.get("to");
        String publicAddress = (String) parameters.get("account");
        String returnUrl = "https://account.liquichain.io/";
		log.info("orderId from setter :{}",orderId);
        String path = this.endpointRequest.getPathInfo();
        if(path.lastIndexOf("/")==16){
        	orderId = path.substring(17);
        }
        log.info("execute paymentScript, orderId={}  account=0x{}, path={}, lastIndex:{}",orderId,publicAddress,path,path.lastIndexOf("/"));
        List<OrderItem> orderItems = new ArrayList<>();

        if (from != null) {
            OrderItem orderItem = new OrderItem();
            orderItem.setCurrencyCode("" + from.get("currency"));
            orderItem.setValue("" + from.get("amount"));
            orderItems.add(orderItem);
        }

        Order order = null;
        Gson gson = new Gson();
        OrderService orderService = new OrderService();

        if (orderId == null || orderId.equals("0")) {
            if (StringUtils.isNotBlank(publicAddress)) {
                try {
                    Wallet toWallet = crossStorageApi.find(defaultRepo, Wallet.class).by("hexHash", publicAddress.toLowerCase()).getResult();
                    Wallet fromWallet = crossStorageApi.find(defaultRepo, Wallet.class).by("hexHash", originWallet).getResult();
                    BigInteger amount = new BigDecimal(to.get("amount").toString()).movePointRight(18).toBigInteger();
				    BigInteger originBalance = new BigInteger(fromWallet.getBalance());
        			log.info("originWallet 0x{} old balance:{} amount:{}",originWallet,fromWallet.getBalance(),amount);
        			if(amount.compareTo(originBalance)<=0){
                      	log.info("create paypal order");
                        order = orderService.createOrder(orderItems, returnUrl);
                      	log.info("return orderId :{}",order.id());
                        PaypalOrder paypalOrder = new PaypalOrder();
                        paypalOrder.setCreationDate(Instant.now());
                        paypalOrder.setOrderId(order.id());
                        paypalOrder.setFromWallet(originWallet);
                        paypalOrder.setFromCurrency(from.get("currency").toString());
                        paypalOrder.setFromAmount(from.get("amount").toString());
                        paypalOrder.setToWallet(publicAddress);
                        //FIXME; use conversion rate
                        paypalOrder.setToCurrency(to.get("currency").toString());
                        paypalOrder.setToAmount(to.get("amount").toString());
                		paypalOrder.setStatus("CREATED");
        				crossStorageApi.createOrUpdate(defaultRepo, paypalOrder);
                        result = gson.toJson(order);
                      	log.info("persisted paypalOrder, result order:{}",result);
        			} else {
                      	log.error("Insufficient global balance");
                        result = createErrorResponse(null, "501", "Insufficient global balance");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    result = createErrorResponse(null, "-32700", e.getMessage());
                }
            } else {
                log.error("account publicAddress:{}",publicAddress);
                result = createErrorResponse(null, "404", message);
            }
        } else {
            log.info("capture {}",orderId);
            order = orderService.captureOrder(orderId);
			PaypalOrder paypalOrder = crossStorageApi.find(defaultRepo, PaypalOrder.class).by("orderId", orderId).getResult();
            if (order == null || paypalOrder==null) {
                message = "Cannot capture order:"+orderId;
                if(paypalOrder!=null){
                	paypalOrder.setStatus("KO");
                    paypalOrder.setError(message);
                    try{
        				crossStorageApi.createOrUpdate(defaultRepo, paypalOrder);
                    } catch (Exception e){
                      e.printStackTrace();
                    }
                }
                result = createErrorResponse(orderId, "406", message);
            } else if (StringUtils.isNotBlank(order.status()) && "COMPLETED".equalsIgnoreCase(order.status())) {
                Transaction transac = new Transaction();
        		transac.setHexHash(orderId);
        		transac.setFromHexHash(originWallet);
        		transac.setToHexHash(paypalOrder.getToWallet());
        		
        		//FIXME: increment the nonce
        		transac.setNonce("1");
      
        		transac.setGasPrice("0");
        		transac.setGasLimit("0");
        		transac.setValue(paypalOrder.getToAmount());
              
        		//FIXME: sign the transaction
        		transac.setSignedHash(UUID.randomUUID().toString());
              
        		transac.setCreationDate(java.time.Instant.now());
                try {
        			crossStorageApi.createOrUpdate(defaultRepo, transac);

                	//FIXME: you should get the BlockForgerScript from scriptService
        			BlockForgerScript.addTransaction(transac);
                    paypalOrder.setStatus("OK");
                  	try {
        		   		crossStorageApi.createOrUpdate(defaultRepo, paypalOrder);
               		} catch(Exception ex){
                  		ex.printStackTrace();
                	}
                    result = "Success";
                    result = createResponse(orderId, null);
                } catch(Exception e){
                    log.error("Paypal ok but transaction ko:{}",order);
                    message = "Transaction error:"+e.getMessage();
                	paypalOrder.setStatus("ALERT");
                	paypalOrder.setError(message);
                	try{
        				crossStorageApi.createOrUpdate(defaultRepo, paypalOrder);
                	} catch (Exception ex){
                    	e.printStackTrace();
                	}
                	result = createErrorResponse(orderId, "406", message);
                }

            } else {
                message = "Capture failed:"+order.status();
                paypalOrder.setStatus("KO");
                paypalOrder.setError(message);
                try{
        			crossStorageApi.createOrUpdate(defaultRepo, paypalOrder);
                } catch (Exception e){
                    e.printStackTrace();
                }
                result = createErrorResponse(orderId, "406", message);
            }
        }

    }

    private String createResponse(String requestId, Order order) {
        String res = "{\n";
        res += "  \"id\": \"" + requestId + "\",\n";
        res += "  \"status\": \"" + 200 + "\",\n";
       // res += "  \"jsonrpc\": \"2.0\",\n";
        if (order != null) {
            try {
                res += "  \"result\": " + new ObjectMapper().writeValueAsString(order) + "\n";
            } catch (JsonProcessingException jpe) {
                // used error code from https://github.com/claudijo/json-rpc-error
                return createErrorResponse(requestId, "-32700", jpe.getMessage());
            }
        } else {
            res += "  \"message\": \"" + result + "\"\n";
        }
        res += "}";
        // log.info("res:{}", res);
        return res;

    }

    private String createErrorResponse(String requestId, String errorCode, String message) {
        String res = "{\n";
        if (requestId != null) {
            res += "  \"id\": \""  + requestId + "\",\n";
        }
        res += "  \"status\": \"" + errorCode + "\",\n";
        res += "  \"message\": \"" + message + "\"\n";
        res += "}";
        log.info("err:{}", res);
        return res;
    }

}

class PaypalSandboxClient {
    static String clientId = "AWVIDI2xMJE0AXDOdEtttOW0WgrLzeNWBUAKClN4bVYXdeP2Hkx3BXPlXOahZs0palbyhcpzrow9ZMg3";
    static String secret = "EOHmSfHQQxACD94zzZOBqPXy3ETALxOTdpr-KRLw4ECRs0Bk3olEhn9AQTz922J6o3U5L47Se5x727l_";

    private static PayPalEnvironment environment = new PayPalEnvironment.Sandbox(clientId, secret);
    static PayPalHttpClient client = new PayPalHttpClient(environment);
}

class PaypalLiveClient {
    static String clientId = "PRODUCTION-CLIENT-ID";
    static String secret = "PRODUCTION-CLIENT-SECRET";
    private static PayPalEnvironment environment = new PayPalEnvironment.Live(clientId, secret);
    static PayPalHttpClient client = new PayPalHttpClient(environment);
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

class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private PayPalHttpClient client = null;
    private boolean debug = false;

    public OrderService() {
        this.client = PaypalSandboxClient.client;
        this.debug = true;
    }

    public OrderService(String type) {
        if ("PRODUCTION".equals(type)) {
            this.client = PaypalLiveClient.client;
        } else {
            this.client = PaypalSandboxClient.client;
            this.debug = true;
        }
    }

    public PayPalHttpClient getClient() {
        return client;
    }

    public void setClient(PayPalHttpClient client) {
        this.client = client;
    }

    public Order createOrder(List<OrderItem> orderItems, String returnUrl) {
        Order order = null;
        boolean hasOrderItems = orderItems != null && !orderItems.isEmpty();
        if (hasOrderItems) {
            OrderRequest orderRequest = new OrderRequest();
            orderRequest.checkoutPaymentIntent("CAPTURE");

            List<PurchaseUnitRequest> purchaseUnits = new ArrayList<>();

            orderItems.stream().forEach((item) -> purchaseUnits.add(new PurchaseUnitRequest()
                    .amountWithBreakdown(new AmountWithBreakdown().currencyCode(item.getCurrencyCode()).value(item.getValue()))));

            orderRequest.purchaseUnits(purchaseUnits);

            ApplicationContext appContext = new ApplicationContext();
            appContext.returnUrl(returnUrl);
            appContext.cancelUrl(returnUrl);
            orderRequest.applicationContext(appContext);

            OrdersCreateRequest request = new OrdersCreateRequest().requestBody(orderRequest);

            try {
                HttpResponse<Order> response = this.client.execute(request);

                order = response.result();

                if (this.debug) {
                    logger.debug("Order ID: " + order.id());
                    order.links().forEach(link -> logger.debug(link.rel() + " => " + link.method() + ":" + link.href()));
                }

            } catch (IOException ioe) {
                if (ioe instanceof HttpException) {
                    logger.error("Failed to create order.", ioe);
                } else {
                    logger.error("Unknown error while creating order.", ioe);
                }
            }
        } else {
            logger.warn("No order details provided, will not process creation of order.");
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
                logger.debug("Status Code: " + response.statusCode());
                logger.debug("Status: " + order.status());
                logger.debug("Order ID: " + order.id());
                logger.debug("Links: ");
                for (LinkDescription link : order.links()) {
                    logger.debug("\t" + link.rel() + ": " + link.href());
                }
                logger.debug("Capture ids:");
                for (PurchaseUnit purchaseUnit : order.purchaseUnits()) {
                    for (Capture capture : purchaseUnit.payments().captures()) {
                        logger.debug("\t" + capture.id());
                    }
                }
            }

        } catch (IOException ioe) {
            if (ioe instanceof HttpException) {
                logger.error("Failed to capture order.", ioe);
            } else {
                logger.error("Unknown error while capturing order.", ioe);
            }
        }
        return order;
    }
}