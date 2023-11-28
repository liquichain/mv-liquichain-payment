package io.liquichain.rate;

import static org.meveo.commons.utils.StringUtils.isNotBlank;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.api.persistence.CrossStorageRequest;
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

    private String toCurrency;
    private String from;
    private String to;

    private Map<String, Object> result;

    public Map<String, Object> getResult() {
        return result;
    }

    public void setToCurrency(String toCurrency) {
        this.toCurrency = toCurrency;
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

        CrossStorageRequest<TradeHistory> request = crossStorageApi.find(defaultRepo, TradeHistory.class);

        Instant now = Instant.now();
        if (isNotBlank(from)) {
            request.by("fromRange time", Instant.parse(from));
        } else {
            request.by("fromRange time", now);
        }

        if (isNotBlank(to)) {
            request.by("toRange time", Instant.parse(to));
        } else {
            request.by("toRange time", now.minus(1, ChronoUnit.DAYS));
        }

        List<TradeHistory> tradeHistories = request.getResults();

        result = Map.of(
                "from", from,
                "to", to,
                "data", tradeHistories.stream().map(tradeHistory -> Map.of(
                        "timestamp", tradeHistory.getTime().toEpochMilli(),
                        "value", ("USD".equals(toCurrency) ? tradeHistory.getPrice() : tradeHistory.getPriceEuro()),
                        "percentChange", tradeHistory.getPercentChange()
                ))
        );
    }
}
