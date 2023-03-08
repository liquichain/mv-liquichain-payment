package io.liquichain.rate;

import java.util.Map;

import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import io.liquichain.api.payment.ConversionRateScript;


public class ExchangeRateProvider extends Script {
		
    private String fromCurrency;
    private String toCurrency;
    private String fromDate;
    private String toDate;
    private long maxValues;
    private String result;

    public void setFromCurrency(String fromCurrency) {
        this.fromCurrency=fromCurrency;
    }
  
    public void setToCurrency(String toCurrency) {
        this.toCurrency=toCurrency;
    }
  
    public void setFromDate(String fromDate) {
        this.fromDate=fromDate;
    }
  
    public void setToDate(String toDate) {
        this.toDate=toDate;
    }
  
    public void setMaxValues(long maxValues){
        this.maxValues = maxValues;
    }
  
    public String getResult() {
        return result;
    }
  
  
	@Override
	public void execute(Map<String, Object> parameters) throws BusinessException {
      	String toCurrency = "" + parameters.get("toCurrency");
      	if(new String("USD").equals(toCurrency)){
        	result="{"
          +"\"from\":\""+fromDate+"\",\n"
          +"\"to\":\""+toDate+"\",\n"
          +"\"data\":[\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 24)+",\"value\":"+ConversionRateScript.KLUB_TO_USD+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 23)+",\"value\":"+ConversionRateScript.KLUB_TO_USD+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 22)+",\"value\":"+ConversionRateScript.KLUB_TO_USD+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 21)+",\"value\":"+ConversionRateScript.KLUB_TO_USD+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 20)+",\"value\":"+ConversionRateScript.KLUB_TO_USD+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 19)+",\"value\":"+ConversionRateScript.KLUB_TO_USD+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 18)+",\"value\":"+ConversionRateScript.KLUB_TO_USD+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 17)+",\"value\":"+ConversionRateScript.KLUB_TO_USD+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 16)+",\"value\":"+ConversionRateScript.KLUB_TO_USD+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 15)+",\"value\":"+ConversionRateScript.KLUB_TO_USD+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 14)+",\"value\":"+ConversionRateScript.KLUB_TO_USD+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 13)+",\"value\":"+ConversionRateScript.KLUB_TO_USD+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 12)+",\"value\":"+ConversionRateScript.KLUB_TO_USD+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 11)+",\"value\":"+ConversionRateScript.KLUB_TO_USD+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 10)+",\"value\":"+ConversionRateScript.KLUB_TO_USD+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 9)+",\"value\":"+ConversionRateScript.KLUB_TO_USD+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 8)+",\"value\":"+ConversionRateScript.KLUB_TO_USD+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 7)+",\"value\":"+ConversionRateScript.KLUB_TO_USD+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 6)+",\"value\":"+ConversionRateScript.KLUB_TO_USD+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 5)+",\"value\":"+ConversionRateScript.KLUB_TO_USD+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 4)+",\"value\":"+ConversionRateScript.KLUB_TO_USD+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 3)+",\"value\":"+ConversionRateScript.KLUB_TO_USD+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 2)+",\"value\":"+ConversionRateScript.KLUB_TO_USD+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000)+",\"value\":"+ConversionRateScript.KLUB_TO_USD+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+System.currentTimeMillis()+",\"value\":"+ConversionRateScript.KLUB_TO_USD+",\"percentChange\":0.0}\n"
          +"]\n"
          +"}";
        }else{
        	result="{"
          +"\"from\":\""+fromDate+"\",\n"
          +"\"to\":\""+toDate+"\",\n"
          +"\"data\":[\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 24)+",\"value\":"+ConversionRateScript.CONVERSION_RATE.get("KLUB_TO_EUR")+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 23)+",\"value\":"+ConversionRateScript.CONVERSION_RATE.get("KLUB_TO_EUR")+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 22)+",\"value\":"+ConversionRateScript.CONVERSION_RATE.get("KLUB_TO_EUR")+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 21)+",\"value\":"+ConversionRateScript.CONVERSION_RATE.get("KLUB_TO_EUR")+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 20)+",\"value\":"+ConversionRateScript.CONVERSION_RATE.get("KLUB_TO_EUR")+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 19)+",\"value\":"+ConversionRateScript.CONVERSION_RATE.get("KLUB_TO_EUR")+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 18)+",\"value\":"+ConversionRateScript.CONVERSION_RATE.get("KLUB_TO_EUR")+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 17)+",\"value\":"+ConversionRateScript.CONVERSION_RATE.get("KLUB_TO_EUR")+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 16)+",\"value\":"+ConversionRateScript.CONVERSION_RATE.get("KLUB_TO_EUR")+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 15)+",\"value\":"+ConversionRateScript.CONVERSION_RATE.get("KLUB_TO_EUR")+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 14)+",\"value\":"+ConversionRateScript.CONVERSION_RATE.get("KLUB_TO_EUR")+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 13)+",\"value\":"+ConversionRateScript.CONVERSION_RATE.get("KLUB_TO_EUR")+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 12)+",\"value\":"+ConversionRateScript.CONVERSION_RATE.get("KLUB_TO_EUR")+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 11)+",\"value\":"+ConversionRateScript.CONVERSION_RATE.get("KLUB_TO_EUR")+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 10)+",\"value\":"+ConversionRateScript.CONVERSION_RATE.get("KLUB_TO_EUR")+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 9)+",\"value\":"+ConversionRateScript.CONVERSION_RATE.get("KLUB_TO_EUR")+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 8)+",\"value\":"+ConversionRateScript.CONVERSION_RATE.get("KLUB_TO_EUR")+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 7)+",\"value\":"+ConversionRateScript.CONVERSION_RATE.get("KLUB_TO_EUR")+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 6)+",\"value\":"+ConversionRateScript.CONVERSION_RATE.get("KLUB_TO_EUR")+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 5)+",\"value\":"+ConversionRateScript.CONVERSION_RATE.get("KLUB_TO_EUR")+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 4)+",\"value\":"+ConversionRateScript.CONVERSION_RATE.get("KLUB_TO_EUR")+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 3)+",\"value\":"+ConversionRateScript.CONVERSION_RATE.get("KLUB_TO_EUR")+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000 * 2)+",\"value\":"+ConversionRateScript.CONVERSION_RATE.get("KLUB_TO_EUR")+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+(System.currentTimeMillis() - 3600000)+",\"value\":"+ConversionRateScript.CONVERSION_RATE.get("KLUB_TO_EUR")+",\"percentChange\":0.0},\n"
          +"{\"timestamp\":"+System.currentTimeMillis()+",\"value\":"+ConversionRateScript.CONVERSION_RATE.get("KLUB_TO_EUR")+",\"percentChange\":0.0}\n"
          +"]\n"
          +"}";
        }
		
	}
}
