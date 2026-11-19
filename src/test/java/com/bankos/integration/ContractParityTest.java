package com.bankos.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class ContractParityTest {

    @Test
    @DisplayName("Verificar que openapi.yaml no ha sido alterado y coincide con su hash SHA-256 oficial")
    void openApiContractMustMatchOfficialChecksum() throws Exception {
        Path contractPath = Paths.get("openapi.yaml");
        if (!Files.exists(contractPath)) {
            contractPath = Paths.get("/workspace/openapi.yaml");
        }
        assertThat(contractPath).exists();

        Path checksumPath = Paths.get("openapi.sha256");
        if (!Files.exists(checksumPath)) {
            checksumPath = Paths.get("/workspace/openapi.sha256");
        }
        assertThat(checksumPath).exists();

        String expectedChecksum = Files.readString(checksumPath).trim().split("\\s+")[0];

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] fileBytes = Files.readAllBytes(contractPath);
        byte[] hashBytes = digest.digest(fileBytes);
        String actualChecksum = HexFormat.of().formatHex(hashBytes);

        assertThat(actualChecksum)
                .as("El contrato openapi.yaml en backend no coincide con el hash oficial de gobernanza")
                .isEqualTo(expectedChecksum);
    }
}
