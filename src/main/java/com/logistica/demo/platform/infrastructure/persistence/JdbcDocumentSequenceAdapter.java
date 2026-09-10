package com.logistica.demo.platform.infrastructure.persistence;

import com.logistica.demo.platform.api.DocumentNumber;
import com.logistica.demo.platform.api.DocumentSequencePort;
import java.util.Locale;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JdbcDocumentSequenceAdapter implements DocumentSequencePort {

    private static final int NUMBER_WIDTH = 6;

    private final JdbcTemplate jdbcTemplate;

    public JdbcDocumentSequenceAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public DocumentNumber nextDocumentNumber(Long companyId, int fiscalYear, String documentType) {
        validate(companyId, fiscalYear);
        String normalizedType = normalize(documentType, "documentType");
        ensureSequenceExists(companyId, fiscalYear, normalizedType);
        SequenceState sequence = jdbcTemplate.query(
                        """
                        SELECT prefix, current_value
                        FROM platform.document_sequences
                        WHERE company_id = ? AND fiscal_year = ? AND document_type = ?
                        FOR UPDATE
                        """,
                        (rs, rowNum) -> new SequenceState(rs.getString("prefix"), rs.getLong("current_value")),
                        companyId,
                        fiscalYear,
                        normalizedType)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No se pudo bloquear la secuencia documental"));

        long nextValue = sequence.currentValue() + 1;
        jdbcTemplate.update(
                """
                UPDATE platform.document_sequences
                SET current_value = ?, version = version + 1
                WHERE company_id = ? AND fiscal_year = ? AND document_type = ?
                """,
                nextValue,
                companyId,
                fiscalYear,
                normalizedType);

        return new DocumentNumber(
                companyId,
                fiscalYear,
                normalizedType,
                sequence.prefix(),
                nextValue,
                sequence.prefix() + "-" + String.format(Locale.ROOT, "%0" + NUMBER_WIDTH + "d", nextValue));
    }

    @Override
    @Transactional
    public void configureSequence(
            Long companyId,
            int fiscalYear,
            String documentType,
            String prefix,
            long currentValue) {
        validate(companyId, fiscalYear);
        if (currentValue < 0) {
            throw new IllegalArgumentException("currentValue no puede ser negativo");
        }
        String normalizedType = normalize(documentType, "documentType");
        String normalizedPrefix = normalize(prefix, "prefix");
        int updated = jdbcTemplate.update(
                """
                UPDATE platform.document_sequences
                SET prefix = ?, current_value = ?, version = version + 1
                WHERE company_id = ? AND fiscal_year = ? AND document_type = ?
                """,
                normalizedPrefix,
                currentValue,
                companyId,
                fiscalYear,
                normalizedType);
        if (updated == 0) {
            jdbcTemplate.update(
                    """
                    INSERT INTO platform.document_sequences (
                        company_id, fiscal_year, document_type, prefix, current_value
                    ) VALUES (?, ?, ?, ?, ?)
                    """,
                    companyId,
                    fiscalYear,
                    normalizedType,
                    normalizedPrefix,
                    currentValue);
        }
    }

    private void ensureSequenceExists(Long companyId, int fiscalYear, String documentType) {
        try {
            jdbcTemplate.update(
                    """
                    INSERT INTO platform.document_sequences (
                        company_id, fiscal_year, document_type, prefix, current_value
                    ) VALUES (?, ?, ?, ?, 0)
                    """,
                    companyId,
                    fiscalYear,
                    documentType,
                    documentType + "-" + fiscalYear);
        } catch (DuplicateKeyException ignored) {
            // Created by another transaction between the read and insert.
        }
    }

    private void validate(Long companyId, int fiscalYear) {
        if (companyId == null || companyId <= 0) {
            throw new IllegalArgumentException("companyId debe ser positivo");
        }
        if (fiscalYear < 2000 || fiscalYear > 2200) {
            throw new IllegalArgumentException("fiscalYear debe estar entre 2000 y 2200");
        }
    }

    private String normalize(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private record SequenceState(String prefix, long currentValue) {
    }
}
