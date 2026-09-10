package com.logistica.demo.platform.api;

public interface FunctionalAuditPort {

    AuditEvent record(AuditEventCommand command);
}
