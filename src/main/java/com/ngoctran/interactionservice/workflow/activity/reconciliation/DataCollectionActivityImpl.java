package com.ngoctran.interactionservice.workflow.activity.reconciliation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Data Collection Activity Implementation
 * Collects transaction data from multiple sources for reconciliation
 */
@Component
@Slf4j
public class DataCollectionActivityImpl implements DataCollectionActivity {

    @Override
    public DataCollectionResult collectTransactionData(String reconciliationId, String date,
                                                      String type, List<String> dataSources,
                                                      Map<String, Object> config) {
        log.info("Collecting transaction data for reconciliation: {}, date: {}, sources: {}",
                reconciliationId, date, dataSources);

        try {
            Map<String, List<TransactionData>> allTransactionData = new HashMap<>();
            Map<String, Integer> transactionsBySource = new HashMap<>();
            List<String> errors = new ArrayList<>();

            // Collect data from each source
            for (String source : dataSources) {
                try {
                    List<TransactionData> sourceData = collectFromSource(source, date, type, config);
                    allTransactionData.put(source, sourceData);
                    transactionsBySource.put(source, sourceData.size());

                    log.info("Collected {} transactions from source: {}", sourceData.size(), source);

                    // Simulate processing time based on data volume
                    Thread.sleep(500 + (int)(Math.random() * 2000));

                } catch (Exception e) {
                    String error = "Failed to collect from " + source + ": " + e.getMessage();
                    log.error(error, e);
                    errors.add(error);
                    // Continue with other sources
                }
            }

            int totalTransactions = transactionsBySource.values().stream().mapToInt(Integer::intValue).sum();

            boolean success = errors.size() < dataSources.size(); // Success if at least one source worked

            log.info("Data collection completed: {} total transactions from {} sources",
                    totalTransactions, dataSources.size());

            return new DataCollectionResult(success, dataSources, allTransactionData,
                    totalTransactions, transactionsBySource, errors);

        } catch (Exception e) {
            log.error("Data collection failed for reconciliation: {}", reconciliationId, e);
            return new DataCollectionResult(false, dataSources, new HashMap<>(), 0,
                    new HashMap<>(), List.of("Data collection failed: " + e.getMessage()));
        }
    }

    private List<TransactionData> collectFromSource(String source, String date, String type,
                                                   Map<String, Object> config) throws Exception {
        List<TransactionData> transactions = new ArrayList<>();

        switch (source.toUpperCase()) {
            case "INTERNAL_DB":
                transactions = collectFromInternalDB(date, type, config);
                break;
            case "BANK_STATEMENTS":
                transactions = collectFromBankStatements(date, config);
                break;
            case "PAYMENT_GATEWAY":
                transactions = collectFromPaymentGateway(date, config);
                break;
            case "EXTERNAL_API":
                transactions = collectFromExternalAPI(date, config);
                break;
            default:
                throw new IllegalArgumentException("Unknown data source: " + source);
        }

        return transactions;
    }

    private List<TransactionData> collectFromInternalDB(String date, String type, Map<String, Object> config) {
        // Simulate collecting from internal payment database
        List<TransactionData> transactions = new ArrayList<>();
        int transactionCount = 800 + (int)(Math.random() * 400); // 800-1200 transactions

        for (int i = 0; i < transactionCount; i++) {
            String transactionId = "INT-" + date.replace("-", "") + "-" + String.format("%06d", i);
            double amount = 10000 + (Math.random() * 990000); // 10k - 1M VND
            String reference = "REF" + String.format("%010d", i);

            transactions.add(new TransactionData(
                transactionId,
                reference,
                amount,
                "VND",
                date,
                date + "T" + String.format("%02d", 8 + (int)(Math.random() * 12)) + ":00:00",
                "ACC" + String.format("%010d", (int)(Math.random() * 100000)),
                "ACC" + String.format("%010d", (int)(Math.random() * 100000)),
                Math.random() > 0.5 ? "TRANSFER" : "PAYMENT",
                "COMPLETED"
            ));
        }

        log.info("Collected {} transactions from internal database", transactions.size());
        return transactions;
    }

    private List<TransactionData> collectFromBankStatements(String date, Map<String, Object> config) {
        // Simulate collecting from bank statement files
        List<TransactionData> transactions = new ArrayList<>();
        int transactionCount = 700 + (int)(Math.random() * 300); // 700-1000 transactions

        for (int i = 0; i < transactionCount; i++) {
            String transactionId = "BANK-" + date.replace("-", "") + "-" + String.format("%06d", i);
            double amount = 15000 + (Math.random() * 985000); // 15k - 1M VND
            String reference = "BANKREF" + String.format("%010d", i);

            transactions.add(new TransactionData(
                transactionId,
                reference,
                amount,
                "VND",
                date,
                date + "T" + String.format("%02d", 9 + (int)(Math.random() * 10)) + ":00:00",
                "BANKACC" + String.format("%014d", (int)(Math.random() * 1000000000000L)),
                "BANKACC" + String.format("%014d", (int)(Math.random() * 1000000000000L)),
                "BANK_TRANSFER",
                "PROCESSED"
            ));
        }

        log.info("Collected {} transactions from bank statements", transactions.size());
        return transactions;
    }

    private List<TransactionData> collectFromPaymentGateway(String date, Map<String, Object> config) {
        // Simulate collecting from payment gateway (e.g., Momo, ZaloPay)
        List<TransactionData> transactions = new ArrayList<>();
        int transactionCount = 600 + (int)(Math.random() * 200); // 600-800 transactions

        for (int i = 0; i < transactionCount; i++) {
            String transactionId = "GW-" + date.replace("-", "") + "-" + String.format("%06d", i);
            double amount = 20000 + (Math.random() * 980000); // 20k - 1M VND
            String reference = "GWREF" + String.format("%010d", i);

            transactions.add(new TransactionData(
                transactionId,
                reference,
                amount,
                "VND",
                date,
                date + "T" + String.format("%02d", 6 + (int)(Math.random() * 16)) + ":00:00",
                "GWACC" + String.format("%010d", (int)(Math.random() * 100000000)),
                "MERCHANT" + String.format("%010d", (int)(Math.random() * 100000000)),
                Math.random() > 0.3 ? "PAYMENT" : "TOPUP",
                "SUCCESS"
            ));
        }

        log.info("Collected {} transactions from payment gateway", transactions.size());
        return transactions;
    }

    private List<TransactionData> collectFromExternalAPI(String date, Map<String, Object> config) {
        // Simulate collecting from external API
        List<TransactionData> transactions = new ArrayList<>();
        int transactionCount = 200 + (int)(Math.random() * 100); // 200-300 transactions

        for (int i = 0; i < transactionCount; i++) {
            String transactionId = "EXT-" + date.replace("-", "") + "-" + String.format("%06d", i);
            double amount = 50000 + (Math.random() * 950000); // 50k - 1M VND
            String reference = "EXTREF" + String.format("%010d", i);

            transactions.add(new TransactionData(
                transactionId,
                reference,
                amount,
                "VND",
                date,
                date + "T" + String.format("%02d", 10 + (int)(Math.random() * 8)) + ":00:00",
                "EXTACC" + String.format("%012d", (int)(Math.random() * 1000000000000L)),
                "EXTACC" + String.format("%012d", (int)(Math.random() * 1000000000000L)),
                "EXTERNAL_TRANSFER",
                "CONFIRMED"
            ));
        }

        log.info("Collected {} transactions from external API", transactions.size());
        return transactions;
    }
}
