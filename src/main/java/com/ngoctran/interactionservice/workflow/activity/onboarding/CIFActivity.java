package com.ngoctran.interactionservice.workflow.activity.onboarding;

import io.temporal.activity.ActivityInterface;

/**
 * CIF Creation Activity
 * Creates Customer Information File in Core Banking System
 */
@ActivityInterface
public interface CIFActivity {

    /**
     * Create CIF in core banking system
     */
    CIFResult createCIF(String customerId, String caseId, CIFData cifData);

    /**
     * CIF creation result
     */
    class CIFResult {
        private boolean success;
        private String cifNumber;
        private String coreCustomerId;
        private String errorMessage;
        private long createdAt;

        public CIFResult() {}

        public CIFResult(boolean success, String cifNumber, String coreCustomerId,
                        String errorMessage, long createdAt) {
            this.success = success;
            this.cifNumber = cifNumber;
            this.coreCustomerId = coreCustomerId;
            this.errorMessage = errorMessage;
            this.createdAt = createdAt;
        }

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getCifNumber() { return cifNumber; }
        public void setCifNumber(String cifNumber) { this.cifNumber = cifNumber; }

        public String getCoreCustomerId() { return coreCustomerId; }
        public void setCoreCustomerId(String coreCustomerId) { this.coreCustomerId = coreCustomerId; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public long getCreatedAt() { return createdAt; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    }

    /**
     * CIF data for creation
     */
    class CIFData {
        private String fullName;
        private String dateOfBirth;
        private String idNumber;
        private String phoneNumber;
        private String email;
        private String address;
        private String nationality;
        private String occupation;

        public CIFData() {}

        public CIFData(String fullName, String dateOfBirth, String idNumber,
                      String phoneNumber, String email, String address,
                      String nationality, String occupation) {
            this.fullName = fullName;
            this.dateOfBirth = dateOfBirth;
            this.idNumber = idNumber;
            this.phoneNumber = phoneNumber;
            this.email = email;
            this.address = address;
            this.nationality = nationality;
            this.occupation = occupation;
        }

        // Getters and setters
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getDateOfBirth() { return dateOfBirth; }
        public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

        public String getIdNumber() { return idNumber; }
        public void setIdNumber(String idNumber) { this.idNumber = idNumber; }

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }

        public String getNationality() { return nationality; }
        public void setNationality(String nationality) { this.nationality = nationality; }

        public String getOccupation() { return occupation; }
        public void setOccupation(String occupation) { this.occupation = occupation; }
    }
}
