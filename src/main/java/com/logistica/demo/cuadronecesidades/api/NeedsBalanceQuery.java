package com.logistica.demo.cuadronecesidades.api;

import java.util.Optional;

public interface NeedsBalanceQuery {

    Optional<NeedsLineBalance> findAvailableLine(Long companyId, Long needsLineId);
}

