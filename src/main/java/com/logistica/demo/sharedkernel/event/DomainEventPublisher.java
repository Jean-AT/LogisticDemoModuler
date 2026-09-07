package com.logistica.demo.sharedkernel.event;

public interface DomainEventPublisher {

    void publish(DomainEvent event);
}

