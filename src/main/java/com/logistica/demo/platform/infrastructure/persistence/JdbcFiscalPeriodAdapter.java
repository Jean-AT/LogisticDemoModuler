package com.logistica.demo.platform.infrastructure.persistence;

import com.logistica.demo.platform.api.DefineFiscalPeriodCommand;
import com.logistica.demo.platform.api.FiscalPeriod;
import com.logistica.demo.platform.api.FiscalPeriodAdministration;
import com.logistica.demo.platform.api.FiscalPeriodQuery;
import com.logistica.demo.platform.api.FiscalPeriodStatus;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JdbcFiscalPeriodAdapter implements FiscalPeriodQuery, FiscalPeriodAdministration {

    private final JdbcTemplate jdbcTemplate;

    public JdbcFiscalPeriodAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<FiscalPeriod> findByCompanyAndDate(Long companyId, LocalDate date) {
        return jdbcTemplate.query(
                        """
                        SELECT id, company_id, fiscal_year, month, status, starts_on, ends_on
                        FROM platform.fiscal_periods
                        WHERE company_id = ?
                          AND starts_on <= ?
                          AND ends_on >= ?
                        ORDER BY starts_on DESC
                        """,
                        (rs, rowNum) -> new FiscalPeriod(
                                rs.getLong("id"),
                                rs.getLong("company_id"),
                                rs.getInt("fiscal_year"),
                                rs.getInt("month"),
                                FiscalPeriodStatus.valueOf(rs.getString("status")),
                                rs.getDate("starts_on").toLocalDate(),
                                rs.getDate("ends_on").toLocalDate()),
                        companyId,
                        date,
                        date)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<FiscalPeriod> findByCompanyYearAndMonth(Long companyId, int fiscalYear, int month) {
        return jdbcTemplate.query(
                        """
                        SELECT id, company_id, fiscal_year, month, status, starts_on, ends_on
                        FROM platform.fiscal_periods
                        WHERE company_id = ? AND fiscal_year = ? AND month = ?
                        """,
                        (rs, rowNum) -> new FiscalPeriod(
                                rs.getLong("id"),
                                rs.getLong("company_id"),
                                rs.getInt("fiscal_year"),
                                rs.getInt("month"),
                                FiscalPeriodStatus.valueOf(rs.getString("status")),
                                rs.getDate("starts_on").toLocalDate(),
                                rs.getDate("ends_on").toLocalDate()),
                        companyId,
                        fiscalYear,
                        month)
                .stream()
                .findFirst();
    }

    @Override
    public FiscalPeriod requireOpenPeriod(Long companyId, LocalDate date) {
        FiscalPeriod period = findByCompanyAndDate(companyId, date)
                .orElseThrow(() -> new IllegalStateException("No existe periodo fiscal para la fecha indicada"));
        if (!period.isOpen()) {
            throw new IllegalStateException("El periodo fiscal esta cerrado");
        }
        return period;
    }

    @Override
    @Transactional
    public FiscalPeriod definePeriod(DefineFiscalPeriodCommand command) {
        int updated = jdbcTemplate.update(
                """
                UPDATE platform.fiscal_periods
                SET starts_on = ?,
                    ends_on = ?,
                    status = ?,
                    updated_by = ?,
                    updated_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE company_id = ? AND fiscal_year = ? AND month = ?
                """,
                command.startsOn(),
                command.endsOn(),
                command.status().name(),
                command.actor(),
                command.companyId(),
                command.fiscalYear(),
                command.month());
        if (updated == 0) {
            try {
                jdbcTemplate.update(
                        """
                        INSERT INTO platform.fiscal_periods (
                            company_id, fiscal_year, month, status, starts_on, ends_on,
                            created_by, created_at, updated_by, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP)
                        """,
                        command.companyId(),
                        command.fiscalYear(),
                        command.month(),
                        command.status().name(),
                        command.startsOn(),
                        command.endsOn(),
                        command.actor(),
                        command.actor());
            } catch (DuplicateKeyException ignored) {
                return definePeriod(command);
            }
        }
        return findByCompanyYearAndMonth(command.companyId(), command.fiscalYear(), command.month())
                .orElseThrow(() -> new IllegalStateException("No se pudo leer el periodo fiscal definido"));
    }

    @Override
    @Transactional
    public FiscalPeriod openPeriod(Long companyId, int fiscalYear, int month, String actor) {
        return changeStatus(companyId, fiscalYear, month, FiscalPeriodStatus.OPEN, actor);
    }

    @Override
    @Transactional
    public FiscalPeriod closePeriod(Long companyId, int fiscalYear, int month, String actor) {
        return changeStatus(companyId, fiscalYear, month, FiscalPeriodStatus.CLOSED, actor);
    }

    private FiscalPeriod changeStatus(
            Long companyId,
            int fiscalYear,
            int month,
            FiscalPeriodStatus status,
            String actor) {
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("actor es obligatorio");
        }
        int updated = jdbcTemplate.update(
                """
                UPDATE platform.fiscal_periods
                SET status = ?, updated_by = ?, updated_at = CURRENT_TIMESTAMP, version = version + 1
                WHERE company_id = ? AND fiscal_year = ? AND month = ?
                """,
                status.name(),
                actor.trim(),
                companyId,
                fiscalYear,
                month);
        if (updated == 0) {
            throw new IllegalArgumentException("El periodo fiscal no existe");
        }
        return findByCompanyYearAndMonth(companyId, fiscalYear, month)
                .orElseThrow(() -> new IllegalStateException("No se pudo leer el periodo fiscal actualizado"));
    }
}
