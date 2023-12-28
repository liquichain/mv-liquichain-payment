package io.liquichain.rate;

import static java.math.RoundingMode.HALF_UP;
import static org.meveo.commons.utils.StringUtils.isNotBlank;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

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

    private class Point {
        public final double x;
        public final double y;

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private String fromCurrency;
    private String toCurrency;
    private int maxValues;
    private String from;
    private String to;
    private Double epsilon = 0.00001;

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

    public void setEpsilon(Double epsilon) {
        this.epsilon = epsilon;
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        LOG.info("Retrieving exchange rate for currency: {}", toCurrency);
        Instant now = Instant.now();
        Instant from = isNotBlank(this.from) ? Instant.parse(this.from) : now.minus(1, ChronoUnit.DAYS);
        Instant to = isNotBlank(this.to) ? Instant.parse(this.to) : now;

        List<Map<String, Object>> tradeDetails = new ArrayList<>();
        if ("KLC".equals(fromCurrency) || "KLUB".equals(fromCurrency)) {
            CrossStorageRequest<TradeHistory> historyRequest = crossStorageApi.find(defaultRepo, TradeHistory.class)
                                                                              .by("fromRange time", from)
                                                                              .by("toRange time", to)
                                                                              .orderBy("time", true);

            if (maxValues > 0) {
                historyRequest.limit(maxValues);
            }

            List<TradeHistory> tradeHistories = historyRequest.getResults();
            List<TradeHistory> simplifiedHistory = tradeHistories.size() > 100
                    ? simplifyTradeHistory(tradeHistories)
                    : tradeHistories;
            tradeDetails.addAll(mapHistoryByCurrency(simplifiedHistory));
        }

        result = new HashMap<>() {{
            put("from", from);
            put("to", to);
            put("data", tradeDetails);
        }};
    }

    private List<Map<String, Object>> mapHistoryByCurrency(List<TradeHistory> tradeHistories) {
        List<Map<String, Object>> tradeDetails = new ArrayList<>();
        BigDecimal previousPrice = BigDecimal.ZERO;
        for (TradeHistory tradeHistory : tradeHistories) {
            Map<String, Object> details = new HashMap<>();
            details.put("timestamp", tradeHistory.getTime().toEpochMilli());
            String price = "USD".equals(toCurrency)
                    ? tradeHistory.getPrice()
                    : tradeHistory.getPriceEuro();
            details.put("value", price);
            BigDecimal currentPrice = parseDecimal(price);
            Double percentChange = BigDecimal.ZERO.compareTo(previousPrice) < 0
                    ? currentPrice.subtract(previousPrice)
                                  .divide(previousPrice, 9, HALF_UP)
                                  .multiply(parseDecimal("100"))
                                  .doubleValue()
                    : 0;
            details.put("percentChange", percentChange);
            tradeDetails.add(details);
            previousPrice = currentPrice;
        }
        return tradeDetails;
    }

    private List<Point> convertHistoryToPoints(List<TradeHistory> tradeHistories) {
        return tradeHistories.stream()
                             .map(tradeHistory -> new Point(
                                     tradeHistory.getTime().toEpochMilli(),
                                     Double.valueOf("USD".equals(toCurrency)
                                             ? tradeHistory.getPrice()
                                             : tradeHistory.getPriceEuro())
                             ))
                             .collect(Collectors.toList());
    }

    public List<TradeHistory> simplifyTradeHistory(List<TradeHistory> tradeHistories) {
        List<Integer> keep = new ArrayList<>();
        List<TradeHistory> simplifiedList = new ArrayList<>();
        keep.add(0);
        simplifiedList.add(tradeHistories.get(0));

        List<Point> allPoints = convertHistoryToPoints(tradeHistories);
        while (true) {
            Integer split = iterativeSplit(allPoints, keep.get(keep.size() - 1), epsilon);
            if (split == null) {
                return simplifiedList;
            }
            keep.add(split);
            simplifiedList.add(tradeHistories.get(split));
        }
    }

    public Integer iterativeSplit(List<Point> points, int p0, double epsilon) {
        if (p0 >= points.size() - 1) {
            return null;
        }

        BiFunction<Point, Point, double[]> calcXi = (point1, point2) -> {
            double x1 = point1.x;
            double y1 = point1.y;
            double x2 = point2.x;
            double y2 = point2.y;

            double dx = x2 - x1;
            double dy = y2 - y1;

            return new double[] {
                    (dy + epsilon) / dx,
                    (dy - epsilon) / dx
            };
        };

        Point p0Point = points.get(p0);
        Point p1Point = points.get(p0 + 1);

        double[] xi = calcXi.apply(p0Point, p1Point);
        double xiTop = xi[0];
        double xiBottom = xi[1];

        for (int i = p0 + 2; i < points.size(); i++) {
            Point nextPoint = points.get(i);

            double[] currentXi = calcXi.apply(p0Point, nextPoint);

            xiTop = Math.min(xiTop, currentXi[0]);
            xiBottom = Math.max(xiBottom, currentXi[1]);

            if (xiTop < xiBottom) {
                return i - 1;
            }
        }

        return points.size() - 1;
    }

    public BigDecimal parseDecimal(String price) {
        return new BigDecimal(price).setScale(9, HALF_UP);
    }

}
