package com.ngoctran.interactionservice.workflow.activity.reconciliation;

import io.temporal.activity.ActivityInterface;
import java.util.List;
import java.util.Map;

/**
 * Data Collection Activity
 * Collects transaction data from multiple sources for reconciliation
 */
@ActivityInterface
public interface DataCollectionActivity {

    /**
     * Collect transaction data from all configured sources
     */
    DataCollectionResult collectTransactionData(String reconciliationId, String date,
                                              String type, List<String> dataSources,
                                              Map<String, Object> config);

    /**
     * Data collection result
     */
    class DataCollectionResult {
        private boolean success;
        private List<String> dataSources;
        private Map<String, List<TransactionData>> transactionData;
        private int totalTransactions;
        private Map<String, Integer> transactionsBySource;
        private List<String> errors;
        private long collectedAt;

        public DataCollectionResult() {}

        public DataCollectionResult(boolean success, List<String> dataSources,
                                  Map<String, List<TransactionData>> transactionData,
                                  int totalTransactions, Map<String, Integer> transactionsBySource,
                                  List<String> errors) {
            this.success = success;
            this.dataSources = dataSources;
            this.transactionData = transactionData;
            this.totalTransactions = totalTransactions;
            this.transactionsBySource = transactionsBySource;
            this.errors = errors;
            this.collectedAt = System.currentTimeMillis();
        }

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public List<String> getDataSources() { return dataSources; }
        public void setDataSources(List<String> dataSources) { this.dataSources = dataSources; }

        public Map<String, List<TransactionData>> getTransactionData() { return transactionData; }
        public void setTransactionData(Map<String, List<TransactionData>> transactionData) { this.transactionData = transactionData; }

        public int getTotalTransactions() { return totalTransactions; }
        public void setTotalTransactions(int totalTransactions) { this.totalTransactions = totalTransactions; }

        public Map<String, Integer> getTransactionsBySource() { return transactionsBySource; }
        public void setTransactionsBySource(Map<String, Integer> transactionsBySource) { this.transactionsBySource = transactionsBySource; }

        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }

        public long getCollectedAt() { return collectedAt; }
        public void setCollectedAt(long collectedAt) { this.collectedAt = collectedAt; }
    }

    /**
     * Transaction data structure
     */
    class TransactionData {
        private String transactionId;
        private String referenceNumber;
        private double amount;
        private String currency;
        private String transactionDate;
        private String transactionTime;
        private String sourceAccount;
        private String destinationAccount;
        private String transactionType;
        private String status;
        private Map<String, Object> additionalData;

        public TransactionData() {}

        public TransactionData(String transactionId, String referenceNumber, double amount,
                             String currency, String transactionDate, String transactionTime,
                             String sourceAccount, String destinationAccount, String transactionType,
                             String status) {
            this.transactionId = transactionId;
            this.referenceNumber = referenceNumber;
            this.amount = amount;
            this.currency = currency;
            this.transactionDate = transactionDate;
            this.transactionTime = transactionTime;
            this.sourceAccount = sourceAccount;
            this.destinationAccount = destinationAccount;
            this.transactionType = transactionType;
            this.status = status;
        }

        // Getters and setters
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

        public String getReferenceNumber() { return referenceNumber; }
        public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public String getTransactionDate() { return transactionDate; }
        public void setTransactionDate(String transactionDate) { this.transactionDate = transactionDate; }

        public String getTransactionTime() { return transactionTime; }
        public void setTransactionTime(String transactionTime) { this.transactionTime = transactionTime; }

        public String getSourceAccount() { return sourceAccount; }
        public void setSourceAccount(String sourceAccount) { this.sourceAccount = sourceAccount; }

        public String getDestinationAccount() { return destinationAccount; }
        public void setDestinationAccount(String destinationAccount) { this.destinationAccount = destinationAccount; }

        public String getTransactionType() { return transactionType; }
        public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Map<String, Object> getAdditionalData() { return additionalData; }
        public void setAdditionalData(Map<String, Object> additionalData) { this.additionalData = additionalData; }
    }
}
