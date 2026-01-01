package com.ngoctran.interactionservice.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Account Created Event - Published when account is successfully created in
 * Core Banking
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountCreatedEvent {
    private String caseId;
    private String customerId;
    private String accountNumber;
    private String customerName;
    private String accountType;
    private long timestamp;
}
