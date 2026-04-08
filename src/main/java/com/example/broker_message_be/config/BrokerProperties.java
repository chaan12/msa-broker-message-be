package com.example.broker_message_be.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "broker")
public class BrokerProperties {

    private boolean kafkaEnabled = true;
    private boolean schedulersEnabled = true;
    private final Retry retry = new Retry();
    private final Topics topics = new Topics();
    private final Notifications notifications = new Notifications();
    private final Services services = new Services();

    public boolean isKafkaEnabled() {
        return kafkaEnabled;
    }

    public void setKafkaEnabled(boolean kafkaEnabled) {
        this.kafkaEnabled = kafkaEnabled;
    }

    public boolean isSchedulersEnabled() {
        return schedulersEnabled;
    }

    public void setSchedulersEnabled(boolean schedulersEnabled) {
        this.schedulersEnabled = schedulersEnabled;
    }

    public Retry getRetry() {
        return retry;
    }

    public Topics getTopics() {
        return topics;
    }

    public Notifications getNotifications() {
        return notifications;
    }

    public Services getServices() {
        return services;
    }

    public static class Retry {

        private long delayMs = 10000L;
        private int batchSize = 25;

        public long getDelayMs() {
            return delayMs;
        }

        public void setDelayMs(long delayMs) {
            this.delayMs = delayMs;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }
    }

    public static class Topics {

        private String payments = "payments_retry_jobs";
        private String orders = "order_retry_jobs";
        private String products = "product_retry_jobs";
        private int partitions = 1;
        private short replicas = 1;

        public String getPayments() {
            return payments;
        }

        public void setPayments(String payments) {
            this.payments = payments;
        }

        public String getOrders() {
            return orders;
        }

        public void setOrders(String orders) {
            this.orders = orders;
        }

        public String getProducts() {
            return products;
        }

        public void setProducts(String products) {
            this.products = products;
        }

        public int getPartitions() {
            return partitions;
        }

        public void setPartitions(int partitions) {
            this.partitions = partitions;
        }

        public short getReplicas() {
            return replicas;
        }

        public void setReplicas(short replicas) {
            this.replicas = replicas;
        }
    }

    public static class Notifications {

        private String defaultRecipient = "ops@example.com";
        private String from = "no-reply@broker-message-be.local";

        public String getDefaultRecipient() {
            return defaultRecipient;
        }

        public void setDefaultRecipient(String defaultRecipient) {
            this.defaultRecipient = defaultRecipient;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }
    }

    public static class Services {

        private String paymentsUrl = "http://pagos-service:8083/pagos/procesar";
        private String ordersUrl = "http://ordenes-service:8082/ordenes";
        private String productsUrl = "http://productos-service:8081/productos";

        public String getPaymentsUrl() {
            return paymentsUrl;
        }

        public void setPaymentsUrl(String paymentsUrl) {
            this.paymentsUrl = paymentsUrl;
        }

        public String getOrdersUrl() {
            return ordersUrl;
        }

        public void setOrdersUrl(String ordersUrl) {
            this.ordersUrl = ordersUrl;
        }

        public String getProductsUrl() {
            return productsUrl;
        }

        public void setProductsUrl(String productsUrl) {
            this.productsUrl = productsUrl;
        }
    }
}
