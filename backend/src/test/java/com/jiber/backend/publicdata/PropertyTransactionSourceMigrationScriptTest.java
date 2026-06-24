package com.jiber.backend.publicdata;

import com.jiber.backend.publicdata.client.*;
import com.jiber.backend.publicdata.config.*;
import com.jiber.backend.publicdata.dto.*;
import com.jiber.backend.publicdata.mapper.*;
import com.jiber.backend.publicdata.service.*;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PropertyTransactionSourceMigrationScriptTest {

    @Test
    void phaseOneSchemaAndSourceUniqueMigrationUseSameSourceTransactionIdStrategy() throws IOException {
        String phaseOneSchema = read("../db/001_phase1_schema.sql");
        String migration = read("../db/005_property_transaction_source_unique.sql");

        assertThat(phaseOneSchema).contains("source_transaction_id VARCHAR(500) NULL");
        assertThat(phaseOneSchema)
                .contains("UNIQUE KEY uk_transactions_source (source_system, source_transaction_id)");

        assertThat(migration).contains("MODIFY source_transaction_id VARCHAR(500) NULL");
        assertThat(migration)
                .contains("ADD UNIQUE KEY uk_transactions_source (source_system, source_transaction_id)");
    }

    @Test
    void migrationFailsBeforeDdlWhenExistingSourceDuplicatesExist() throws IOException {
        String migration = read("../db/005_property_transaction_source_unique.sql");

        assertThat(migration).contains("SIGNAL SQLSTATE '45000'");
        assertThat(migration).contains("JIBER_PROPERTY_TX_SOURCE_DUPLICATE");
        assertThat(migration).contains("GROUP BY source_system, source_transaction_id");
        assertThat(migration).contains("source_system IS NOT NULL");
        assertThat(migration).contains("source_transaction_id IS NOT NULL");
        assertThat(migration).doesNotContain("MESSAGE_TEXT = CONCAT");
    }

    @Test
    void migrationDocumentsNullPolicyAndIsNotDestructive() throws IOException {
        String migration = read("../db/005_property_transaction_source_unique.sql");
        String migrationUpper = migration.toUpperCase();

        assertThat(migration).contains("NULL policy");
        assertThat(migrationUpper).doesNotContain("DROP TABLE");
        assertThat(migrationUpper).doesNotContain("TRUNCATE");
        assertThat(migrationUpper).doesNotContain("DROP DATABASE");
    }

    @Test
    void backendReadmeDocumentsMigrationOrderAndSourceIdStrategy() throws IOException {
        String readme = read("README.md");

        assertThat(readme).contains("db/005_property_transaction_source_unique.sql");
        assertThat(readme).contains("005 Property Transaction Source Migration");
        assertThat(readme).contains("VARCHAR(500)");
        assertThat(readme).contains("source_system + source_transaction_id");
        assertThat(readme).contains("NULL");
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
