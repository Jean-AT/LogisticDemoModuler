package com.logistica.demo.platform.api;

public interface DocumentSequencePort {

    DocumentNumber nextDocumentNumber(Long companyId, int fiscalYear, String documentType);

    void configureSequence(Long companyId, int fiscalYear, String documentType, String prefix, long currentValue);
}
