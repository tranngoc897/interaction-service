package com.ngoctran.interactionservice.workflow.activity.reconciliation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Matching Activity Implementation
 * Performs transaction matching across different data sources
 */
@Component
@Slf4j
public class MatchingActivityImpl implements MatchingActivity {

    @Override
    public MatchingResult performTransactionMatching(Map<String, List<DataCollectionActivity.TransactionData>> transactionData,
                                                    double matchingTolerance, List<String> matchingRules,
                                                    Map<String, Object> config) {
        log.info("Starting transaction matching across {} sources", transactionData.size());

        try {
            List<MatchedTransaction> matchedTransactions = new ArrayList<>();
            List<Discrepancy> discrepancies = new ArrayList<>();
            Map<String, Integer> matchStats = new HashMap<>();

            // Initialize stats
            matchStats.put("EXACT_MATCH", 0);
            matchStats.put("FUZZY_MATCH", 0);
            matchStats.put("REFERENCE_MATCH", 0);
            matchStats.put("TIME_BASED_MATCH", 0);
            matchStats.put("NO_MATCH", 0);

            // Flatten all transactions for matching
            List<DataCollectionActivity.TransactionData> allTransactions = new ArrayList<>();
            for (List<DataCollectionActivity.TransactionData> sourceData : transactionData.values()) {
                allTransactions.addAll(sourceData);
            }

            log.info("Matching {} total transactions", allTransactions.size());

            // Perform matching based on rules
            for (String rule : matchingRules) {
                switch (rule.toUpperCase()) {
                    case "EXACT":
                        performExactMatching(allTransactions, matchedTransactions, discrepancies, matchStats);
                        break;
                    case "AMOUNT_TIME":
                        performAmountTimeMatching(allTransactions, matchedTransactions, discrepancies,
                                                matchingTolerance, matchStats);
                        break;
                    case "REFERENCE":
                        performReferenceMatching(allTransactions, matchedTransactions, discrepancies, matchStats);
                        break;
                }
            }

            // Mark remaining transactions as discrepancies
            markUnmatchedAsDiscrepancies(allTransactions, discrepancies, matchedTransactions);

            // Update NO_MATCH count
            matchStats.put("NO_MATCH", discrepancies.size() -
                (matchStats.get("EXACT_MATCH") + matchStats.get("FUZZY_MATCH") +
                 matchStats.get("REFERENCE_MATCH") + matchStats.get("TIME_BASED_MATCH")));

            // Simulate processing time
            Thread.sleep(2000 + (int)(Math.random() * 3000));

            log.info("Matching completed: {} matched, {} discrepancies",
                    matchedTransactions.size(), discrepancies.size());

            return new MatchingResult(true, matchedTransactions, discrepancies, matchStats);

        } catch (Exception e) {
            log.error("Transaction matching failed", e);
            return new MatchingResult(false, new ArrayList<>(), new ArrayList<>(),
                    Map.of("ERROR", 1));
        }
    }

    private void performExactMatching(List<DataCollectionActivity.TransactionData> transactions,
                                    List<MatchedTransaction> matchedTransactions,
                                    List<Discrepancy> discrepancies,
                                    Map<String, Integer> matchStats) {
        log.info("Performing exact matching...");

        Map<String, List<DataCollectionActivity.TransactionData>> referenceMap = new HashMap<>();

        // Group by reference number
        for (DataCollectionActivity.TransactionData tx : transactions) {
            if (tx.getReferenceNumber() != null && !tx.getReferenceNumber().isEmpty()) {
                referenceMap.computeIfAbsent(tx.getReferenceNumber(), k -> new ArrayList<>()).add(tx);
            }
        }

        // Find exact matches (same reference, same amount, same date)
        for (Map.Entry<String, List<DataCollectionActivity.TransactionData>> entry : referenceMap.entrySet()) {
            List<DataCollectionActivity.TransactionData> group = entry.getValue();
            if (group.size() >= 2) {
                // Sort by amount to find closest matches
                group.sort((a, b) -> Double.compare(a.getAmount(), b.getAmount()));

                for (int i = 0; i < group.size() - 1; i++) {
                    DataCollectionActivity.TransactionData tx1 = group.get(i);
                    DataCollectionActivity.TransactionData tx2 = group.get(i + 1);

                    if (Math.abs(tx1.getAmount() - tx2.getAmount()) < 1.0 && // Same amount (within 1 VND)
                        tx1.getTransactionDate().equals(tx2.getTransactionDate())) { // Same date

                        // Create matched transaction
                        MatchedTransaction match = new MatchedTransaction(
                            "MATCH-" + tx1.getReferenceNumber(),
                            Arrays.asList(tx1.getTransactionId(), tx2.getTransactionId()),
                            tx1.getAmount(),
                            "EXACT",
                            1.0
                        );
                        matchedTransactions.add(match);
                        matchStats.put("EXACT_MATCH", matchStats.get("EXACT_MATCH") + 1);

                        // Remove from available transactions
                        transactions.remove(tx1);
                        transactions.remove(tx2);
                        i--; // Adjust index after removal
                    }
                }
            }
        }

        log.info("Exact matching found {} matches", matchStats.get("EXACT_MATCH"));
    }

    private void performAmountTimeMatching(List<DataCollectionActivity.TransactionData> transactions,
                                         List<MatchedTransaction> matchedTransactions,
                                         List<Discrepancy> discrepancies,
                                         double tolerance, Map<String, Integer> matchStats) {
        log.info("Performing amount and time-based matching with tolerance: {}%", tolerance * 100);

        // Sort transactions by amount for efficient matching
        transactions.sort((a, b) -> Double.compare(a.getAmount(), b.getAmount()));

        Set<String> matchedIds = new HashSet<>();
        List<MatchedTransaction> newMatches = new ArrayList<>();

        for (int i = 0; i < transactions.size(); i++) {
            if (matchedIds.contains(transactions.get(i).getTransactionId())) continue;

            DataCollectionActivity.TransactionData tx1 = transactions.get(i);
            double targetAmount = tx1.getAmount();

            // Look for matching amount within tolerance
            for (int j = i + 1; j < transactions.size(); j++) {
                if (matchedIds.contains(transactions.get(j).getTransactionId())) continue;

                DataCollectionActivity.TransactionData tx2 = transactions.get(j);
                double amountDiff = Math.abs(tx1.getAmount() - tx2.getAmount());
                double amountTolerance = Math.max(tx1.getAmount(), tx2.getAmount()) * tolerance;

                if (amountDiff <= amountTolerance &&
                    tx1.getTransactionDate().equals(tx2.getTransactionDate())) {

                    // Check if accounts are different (valid transfer)
                    if (!tx1.getSourceAccount().equals(tx2.getSourceAccount()) ||
                        !tx1.getDestinationAccount().equals(tx2.getDestinationAccount())) {

                        double confidence = 1.0 - (amountDiff / amountTolerance);
                        String matchType = amountDiff < 1.0 ? "EXACT" : "FUZZY";

                        MatchedTransaction match = new MatchedTransaction(
                            "MATCH-" + System.nanoTime(),
                            Arrays.asList(tx1.getTransactionId(), tx2.getTransactionId()),
                            (tx1.getAmount() + tx2.getAmount()) / 2, // Average amount
                            matchType,
                            confidence
                        );
                        newMatches.add(match);

                        matchedIds.add(tx1.getTransactionId());
                        matchedIds.add(tx2.getTransactionId());

                        matchStats.put(matchType.equals("EXACT") ? "EXACT_MATCH" : "FUZZY_MATCH",
                                     matchStats.get(matchType.equals("EXACT") ? "EXACT_MATCH" : "FUZZY_MATCH") + 1);
                        break;
                    }
                }
            }
        }

        // Remove matched transactions
        transactions.removeIf(tx -> matchedIds.contains(tx.getTransactionId()));
        matchedTransactions.addAll(newMatches);

        log.info("Amount/time matching found {} matches", newMatches.size());
    }

    private void performReferenceMatching(List<DataCollectionActivity.TransactionData> transactions,
                                        List<MatchedTransaction> matchedTransactions,
                                        List<Discrepancy> discrepancies,
                                        Map<String, Integer> matchStats) {
        log.info("Performing reference-based matching...");

        Map<String, List<DataCollectionActivity.TransactionData>> referenceMap = new HashMap<>();

        // Group by reference number (partial matching)
        for (DataCollectionActivity.TransactionData tx : transactions) {
            if (tx.getReferenceNumber() != null && !tx.getReferenceNumber().isEmpty()) {
                // Use last 8 characters for partial matching
                String partialRef = tx.getReferenceNumber().length() > 8 ?
                    tx.getReferenceNumber().substring(tx.getReferenceNumber().length() - 8) :
                    tx.getReferenceNumber();

                referenceMap.computeIfAbsent(partialRef, k -> new ArrayList<>()).add(tx);
            }
        }

        List<MatchedTransaction> newMatches = new ArrayList<>();
        Set<String> matchedIds = new HashSet<>();

        for (Map.Entry<String, List<DataCollectionActivity.TransactionData>> entry : referenceMap.entrySet()) {
            List<DataCollectionActivity.TransactionData> group = entry.getValue();
            if (group.size() >= 2) {
                // Simple matching: pair first two with similar amounts
                DataCollectionActivity.TransactionData tx1 = group.get(0);
                DataCollectionActivity.TransactionData tx2 = group.get(1);

                double amountDiff = Math.abs(tx1.getAmount() - tx2.getAmount());
                double avgAmount = (tx1.getAmount() + tx2.getAmount()) / 2;
                double tolerance = avgAmount * 0.05; // 5% tolerance

                if (amountDiff <= tolerance) {
                    MatchedTransaction match = new MatchedTransaction(
                        "REF-MATCH-" + entry.getKey(),
                        Arrays.asList(tx1.getTransactionId(), tx2.getTransactionId()),
                        avgAmount,
                        "REFERENCE",
                        0.8 // Lower confidence for reference matching
                    );
                    newMatches.add(match);
                    matchedIds.add(tx1.getTransactionId());
                    matchedIds.add(tx2.getTransactionId());
                }
            }
        }

        // Remove matched transactions
        transactions.removeIf(tx -> matchedIds.contains(tx.getTransactionId()));
        matchedTransactions.addAll(newMatches);
        matchStats.put("REFERENCE_MATCH", matchStats.get("REFERENCE_MATCH") + newMatches.size());

        log.info("Reference matching found {} matches", newMatches.size());
    }

    private void markUnmatchedAsDiscrepancies(List<DataCollectionActivity.TransactionData> transactions,
                                            List<Discrepancy> discrepancies,
                                            List<MatchedTransaction> matchedTransactions) {
        log.info("Marking {} unmatched transactions as discrepancies", transactions.size());

        Set<String> matchedIds = new HashSet<>();
        for (MatchedTransaction match : matchedTransactions) {
            matchedIds.addAll(match.getSourceTransactions());
        }

        for (DataCollectionActivity.TransactionData tx : transactions) {
            if (!matchedIds.contains(tx.getTransactionId())) {
                // Determine discrepancy type
                String discrepancyType = determineDiscrepancyType(tx);
                String severity = determineSeverity(tx);

                Discrepancy discrepancy = new Discrepancy(
                    "DISC-" + System.nanoTime(),
                    tx.getTransactionId(),
                    extractSourceFromTransactionId(tx.getTransactionId()),
                    discrepancyType,
                    tx.getAmount(),
                    tx.getAmount(), // Expected = actual for unmatched
                    tx.getTransactionDate(),
                    tx.getTransactionDate(),
                    "Transaction not found in other systems",
                    severity
                );

                discrepancies.add(discrepancy);
            }
        }

        log.info("Created {} discrepancy records", discrepancies.size());
    }

    private String determineDiscrepancyType(DataCollectionActivity.TransactionData tx) {
        // Logic to determine discrepancy type based on transaction data
        if (tx.getStatus().equals("FAILED")) {
            return "FAILED_TRANSACTION";
        } else if (tx.getAmount() > 10000000) { // > 10M VND
            return "HIGH_VALUE_MISSING";
        } else {
            return "MISSING_TRANSACTION";
        }
    }

    private String determineSeverity(DataCollectionActivity.TransactionData tx) {
        if (tx.getAmount() > 50000000) { // > 50M VND
            return "CRITICAL";
        } else if (tx.getAmount() > 10000000) { // > 10M VND
            return "HIGH";
        } else if (tx.getAmount() > 1000000) { // > 1M VND
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    private String extractSourceFromTransactionId(String transactionId) {
        if (transactionId.startsWith("INT-")) return "INTERNAL_DB";
        if (transactionId.startsWith("BANK-")) return "BANK_STATEMENTS";
        if (transactionId.startsWith("GW-")) return "PAYMENT_GATEWAY";
        if (transactionId.startsWith("EXT-")) return "EXTERNAL_API";
        return "UNKNOWN";
    }
}
