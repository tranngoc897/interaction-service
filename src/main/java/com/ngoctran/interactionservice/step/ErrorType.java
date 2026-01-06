package com.ngoctran.interactionservice.step;

public enum ErrorType {
    TRANSIENT,   // Auto retry (network, timeout)
    BUSINESS,    // Stop + manual review (validation, AML hit)
    SYSTEM       // Stop + incident (bug, schema error)
}
