package com.cmt.e2e.tests.start.migration.cubrid;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.cmt.e2e.framework.db.driver.Drivers.DB.CUBRID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cmt.e2e.framework.assertion.DatabaseAsserts;
import com.cmt.e2e.framework.assertion.strategies.MigrationSummaryVerificationStrategy;
import com.cmt.e2e.framework.command.CommandResult;
import com.cmt.e2e.framework.command.impls.StartCommand;
import com.cmt.e2e.framework.core.CmtE2eTestBase;
import com.cmt.e2e.framework.db.containers.CubridContainer;
import com.cmt.e2e.framework.db.containers.DatabaseContainer;
import com.cmt.e2e.framework.db.driver.Drivers;
import com.cmt.e2e.framework.junit.annotation.TestResources;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;

@DisplayName("CUBRID to CUBRID Migration Test")
public class CubridToCubridTest extends CmtE2eTestBase {

    @Container
    private final DatabaseContainer sourceDb = CubridContainer.withDemodb();

    @Container
    private final DatabaseContainer targetDb = CubridContainer.withEmptyDb();

    @Test
    @TestResources("start/migration/cubrid/cubrid_to_cubrid")
    @DisplayName("CUBRID(demodb) to CUBRID(empty) Migration")
    void cubridToCubridMigration() throws IOException, InterruptedException {
        // Arrange
        Path scriptTemplate = testPaths.getResourceDir().resolve("script.xml");
        String scriptContent = Files.readString(scriptTemplate)
            .replace("%%SOURCE_HOST%%", sourceDb.getHost())
            .replace("%%SOURCE_PORT%%", sourceDb.getDatabasePort().toString())
            .replace("%%SOURCE_DRIVER%%", Drivers.latest(CUBRID).toAbsolutePath().toString())
            .replace("%%TARGET_HOST%%", targetDb.getHost())
            .replace("%%TARGET_PORT%%", targetDb.getDatabasePort().toString())
            .replace("%%TARGET_DRIVER%%", Drivers.latest(CUBRID).toAbsolutePath().toString());

        Path scriptPath = testPaths.artifactDir.resolve("CUBRID_to_CUBRID.xml");
        Files.writeString(scriptPath, scriptContent);

        // Act
        StartCommand command = StartCommand.builder().script(scriptPath).build();
        CommandResult result = commandRunner.run(command);

        // Assert
        assertEquals(0, result.exitCode(), "Migration should complete with exit code 0 for success.");

        verifier.verifyWith(result, "expected_summary.answer", new MigrationSummaryVerificationStrategy());

        DatabaseAsserts.assertRecordCount(targetDb, "cubdb", "athlete", 6677);
        DatabaseAsserts.assertRecordCount(targetDb, "cubdb", "code", 6);
        DatabaseAsserts.assertRecordCount(targetDb, "cubdb", "event", 422);
        DatabaseAsserts.assertRecordCount(targetDb, "cubdb", "game", 8653);
        DatabaseAsserts.assertRecordCount(targetDb, "cubdb", "history", 147);
        DatabaseAsserts.assertRecordCount(targetDb, "cubdb", "nation", 215);
        DatabaseAsserts.assertRecordCount(targetDb, "cubdb", "code", 6);
        DatabaseAsserts.assertRecordCount(targetDb, "cubdb", "olympic", 25);
        DatabaseAsserts.assertRecordCount(targetDb, "cubdb", "participant", 916);
        DatabaseAsserts.assertRecordCount(targetDb, "cubdb", "record", 2000);
        DatabaseAsserts.assertRecordCount(targetDb, "cubdb", "stadium", 141);
    }
}
