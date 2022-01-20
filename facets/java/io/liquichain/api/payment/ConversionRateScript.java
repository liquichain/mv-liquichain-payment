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

    public static BigDecimal LCN_TO_EUR = (new BigDecimal(2000)).setScale(9, RoundingMode.HALF_UP);
    public static BigDecimal EUR_TO_LCN = new BigDecimal(0.0005).setScale(9, RoundingMode.HALF_UP);
    public static BigDecimal KLC_TO_EUR = new BigDecimal(1000).setScale(9, RoundingMode.HALF_UP);
    public static BigDecimal EUR_TO_KLC = new BigDecimal(0.001).setScale(9, RoundingMode.HALF_UP);
  
	@Override
	public void execute(Map<String, Object> parameters) throws BusinessException {
		result="{\"data\":[\n"
          +"{\"from\":{\"value\":1,\"currency\":\"LCN\"},\"to\":{\"value\":"+LCN_TO_EUR+",\"currency\":\"EUR\"}},\n"
          +"{\"from\":{\"value\":1,\"currency\":\"EUR\"},\"to\":{\"value\":"+EUR_TO_LCN+",\"currency\":\"LCN\"}},\n"
          +"{\"from\":{\"value\":1,\"currency\":\"KLC\"},\"to\":{\"value\":"+KLC_TO_EUR+",\"currency\":\"EUR\"}},\n"
          +"{\"from\":{\"value\":1,\"currency\":\"EUR\"},\"to\":{\"value\":"+EUR_TO_KLC+",\"currency\":\"KLC\"}}\n"
          +"],\n"
          +"\"timestamp\":"+System.currentTimeMillis()+"\n"
          +"}";
	}
	
}