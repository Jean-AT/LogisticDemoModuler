package com.logistica.demo.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class MigrationCatalogTest {

    private static final Path MIGRATION_DIRECTORY =
            Path.of("src", "main", "resources", "db", "migration", "postgresql");
    private static final Pattern VERSIONED_MIGRATION = Pattern.compile("V([1-9][0-9]*)__([a-z0-9_]+)\\.sql");

    @Test
    void shouldKeepCanonicalMigrationChainContiguousAndNonEmpty() throws IOException {
        List<Path> migrationFiles;
        try (var paths = Files.list(MIGRATION_DIRECTORY)) {
            migrationFiles = paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith("V"))
                    .toList();
        }

        List<Integer> versions = new ArrayList<>();
        Set<Integer> uniqueVersions = new HashSet<>();
        for (Path migrationFile : migrationFiles) {
            String fileName = migrationFile.getFileName().toString();
            Matcher matcher = VERSIONED_MIGRATION.matcher(fileName);
            assertTrue(matcher.matches(), () -> "Nombre Flyway invalido: " + fileName);

            int version = Integer.parseInt(matcher.group(1));
            assertTrue(uniqueVersions.add(version), () -> "Version Flyway duplicada: V" + version);

            String sql = Files.readString(migrationFile).trim();
            assertFalse(sql.isEmpty(), () -> "Migracion vacia: " + fileName);
            assertTrue(sql.contains(";"), () -> "Migracion sin sentencia SQL terminada: " + fileName);
            versions.add(version);
        }

        assertFalse(versions.isEmpty(), "Debe existir al menos una migracion PostgreSQL");
        versions.sort(Integer::compareTo);
        assertEquals(
                java.util.stream.IntStream.rangeClosed(1, versions.get(versions.size() - 1)).boxed().toList(),
                versions,
                "La cadena Flyway debe ser contigua");
    }
}
