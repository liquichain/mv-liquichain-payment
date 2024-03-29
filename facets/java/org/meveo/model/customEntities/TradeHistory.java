package org.meveo.model.customEntities;

import org.meveo.model.CustomEntity;
import java.io.Serializable;
import java.util.List;
import org.meveo.model.persistence.DBStorageType;
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class TradeHistory implements CustomEntity, Serializable {

    public TradeHistory() {
    }

    public TradeHistory(String uuid) {
        this.uuid = uuid;
    }

    private String uuid;

    @JsonIgnore()
    private DBStorageType storages;

    private Double percentChange;

    private String side;

    private String priceEuro;

    private String size;

    private String price;

    private Instant time;

    @Override()
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public DBStorageType getStorages() {
        return storages;
    }

    public void setStorages(DBStorageType storages) {
        this.storages = storages;
    }

    public Double getPercentChange() {
        return percentChange;
    }

    public void setPercentChange(Double percentChange) {
        this.percentChange = percentChange;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public String getPriceEuro() {
        return priceEuro;
    }

    public void setPriceEuro(String priceEuro) {
        this.priceEuro = priceEuro;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public Instant getTime() {
        return time;
    }

    public void setTime(Instant time) {
        this.time = time;
    }

    @Override()
    public String getCetCode() {
        return "TradeHistory";
    }
}
