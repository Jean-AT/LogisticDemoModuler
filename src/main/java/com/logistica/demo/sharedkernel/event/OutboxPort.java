package com.logistica.demo.sharedkernel.event;

public interface OutboxPort {

    void append(DomainEvent event);
}

