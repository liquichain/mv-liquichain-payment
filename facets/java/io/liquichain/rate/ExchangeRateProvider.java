package io.liquichain.rate;

import static org.meveo.commons.utils.StringUtils.isNotBlank;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.model.customEntities.TradeHistory;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.service.storage.RepositoryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExchangeRateProvider extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(ExchangeRateProvider.class);

    private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private final Repository defaultRepo = repositoryService.findDefaultRepository();

    private String fromCurrency;
    private String toCurrency;
    private int maxValues = 100;
    private String from;
    private String to;

    private Map<String, Object> result;

    public Map<String, Object> getResult() {
        return result;
    }

    public void setFromCurrency(String fromCurrency) {
        this.fromCurrency = fromCurrency;
    }

    public void setToCurrency(String toCurrency) {
        this.toCurrency = toCurrency;
    }

    public void setMaxValues(int maxValues) {
        this.maxValues = maxValues;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setTo(String to) {
        this.to = to;
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        LOG.info("Retrieving exchange rate for currency: {}", toCurrency);
        Instant now = Instant.now();
        Instant from = isNotBlank(this.from) ? Instant.parse(this.from) : now.minus(1, ChronoUnit.DAYS);
        Instant to = isNotBlank(this.to) ? Instant.parse(this.to) : now;

        List<Map<String, Object>> tradeDetails;
        if ("KLUB".equals(fromCurrency)) {
            List<TradeHistory> tradeHistories = crossStorageApi.find(defaultRepo, TradeHistory.class)
                                                               .by("fromRange time", from)
                                                               .by("toRange time", to)
                                                               .orderBy("time", true)
                                                               .limit(maxValues)
                                                               .getResults();

            tradeDetails = tradeHistories.stream().map(tradeHistory -> {
                Map<String, Object> details = new HashMap<>();
                details.put("timestamp", tradeHistory.getTime().toEpochMilli());
                details.put("value", "USD".equals(toCurrency) ? tradeHistory.getPrice() : tradeHistory.getPriceEuro());
                details.put("percentChange", tradeHistory.getPercentChange());
                return details;
            }).collect(Collectors.toList());
        } else {
            tradeDetails = new ArrayList<>();
        }

        result = new HashMap<>() {{
            put("from", from);
            put("to", to);
            put("data", tradeDetails);
        }};
    }
}
