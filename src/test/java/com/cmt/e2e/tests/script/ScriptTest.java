package com.cmt.e2e.tests.script;

import com.cmt.e2e.assertion.strategies.XmlVerificationStrategy;
import com.cmt.e2e.command.CommandResult;
import com.cmt.e2e.command.impls.ScriptCommand;
import com.cmt.e2e.support.CmtE2eTestBase;
import com.cmt.e2e.support.Drivers;
import com.cmt.e2e.support.TestLogHolder;
import com.cmt.e2e.support.annotation.TestResources;
import com.cmt.e2e.support.containers.CubridDemodbContainer;
import com.cmt.e2e.support.jdbc.CubridJdbcUrlStrategy;
import com.cmt.e2e.support.jdbc.JdbcPreflight;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import static com.cmt.e2e.support.Drivers.DB.CUBRID;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ScriptTest extends CmtE2eTestBase {

    private static final String DB_CONF_FILENAME = "db.conf";
    private static final String SCRIPT_ANSWER_FILENAME = "script.answer";
    private static final String CUBRID_SOURCE_NAME = "cubrid_source";
    private static final String FILE_TARGET_NAME = "file_target";
    private static final String GENERATED_SCRIPT_PREFIX = "CUBRID_demodb_";

    @TempDir
    Path tempDir;

    @Test
    @TestResources("script")
    @DisplayName("온라인 CUBRID DB를 소스로 마이그레이션 스크립트를 생성한다")
    void should_generateMigrationScript_when_sourceIsCUBRIDOnline() throws Exception {
        // Arrange
        prepareDatabaseAndConfiguration();

        ScriptCommand command = ScriptCommand.builder()
                .source(CUBRID_SOURCE_NAME)
                .target(FILE_TARGET_NAME)
                .output(tempDir.toString())
                .build();

        // Act
        CommandResult result = commandRunner.run(command);

        // Assert
        assertEquals(0, result.exitCode(), "Script command should succeed");
        verifyGeneratedScript(result);
    }

    private void prepareDatabaseAndConfiguration() throws IOException, InterruptedException {
        Path resourceDbConf = testPaths.getResourceDir().resolve(DB_CONF_FILENAME);

        CubridDemodbContainer.waitUntilReady();

        CubridDemodbContainer.patchDbConfHost(resourceDbConf, CUBRID_SOURCE_NAME);
        CubridDemodbContainer.patchDbConfPort(resourceDbConf, CUBRID_SOURCE_NAME);
        CubridDemodbContainer.patchDbConfDriver(resourceDbConf, CUBRID_SOURCE_NAME, Drivers.latest(CUBRID));
        CubridDemodbContainer.patchDbConfOutput(resourceDbConf, FILE_TARGET_NAME, testPaths.artifactDir);

        TestLogHolder.log("[RUNNER] workDir=" + cmtConsoleWorkDir.getPath());
        workspaceFixtures.copyConfToWorkspace(resourceDbConf);
        Path finalDbConf = cmtConsoleWorkDir.toPath().resolve(DB_CONF_FILENAME);

        String host = readProp(finalDbConf, CUBRID_SOURCE_NAME + ".host", "localhost");
        int port = Integer.parseInt(readProp(finalDbConf, CUBRID_SOURCE_NAME + ".port", "33000"));
        String dbname = readProp(finalDbConf, CUBRID_SOURCE_NAME + ".dbname", "demodb");
        String user = readProp(finalDbConf, CUBRID_SOURCE_NAME + ".user", "public");
        String pass = readProp(finalDbConf, CUBRID_SOURCE_NAME + ".password", "");
        String charset = readProp(finalDbConf, CUBRID_SOURCE_NAME + ".charset", "utf-8");

        TestLogHolder.log("--- FINAL db.conf ---");
        Files.lines(finalDbConf).forEach(TestLogHolder::log);
        TestLogHolder.log("---------------------");

        TestLogHolder.log("[NET] host.docker.internal = " + InetAddress.getByName("host.docker.internal").getHostAddress());
        try (var s = new Socket(CubridDemodbContainer.getHost(), CubridDemodbContainer.getMappedBrokerPort())) {
            TestLogHolder.log("[NET] TCP OK to demodb");
        } catch (Exception e) {
            throw new AssertionError("[NET] TCP FAIL to demodb: " +
                    CubridDemodbContainer.getHost() + ":" + CubridDemodbContainer.getMappedBrokerPort(), e);
        }

        Path driverJar = Drivers.latest(CUBRID);
        JdbcPreflight.run(driverJar, new CubridJdbcUrlStrategy(), host, port, dbname, user, pass, charset);
    }

    private void verifyGeneratedScript(CommandResult result) throws IOException {
        TestLogHolder.log("[RUN-RESULT] exit=%d, timedOut=%s%n%s%n",
                result.exitCode(), result.timedOut(), result.output());

        String tempDirContents = Files.list(tempDir).map(Path::toString).collect(Collectors.joining("\n"));
        TestLogHolder.log("[TEMP] tempDir contents:\n" + tempDirContents);

        Path generatedScriptFile = testPaths.findGeneratedScriptFile(tempDir, GENERATED_SCRIPT_PREFIX)
                .orElseThrow(() -> new AssertionError("Generated script file not found in " + tempDir));

        String actualXml = Files.readString(generatedScriptFile);
        CommandResult verificationResult = new CommandResult(actualXml, result.exitCode(), result.timedOut());
        verifier.verifyWith(verificationResult, SCRIPT_ANSWER_FILENAME, new XmlVerificationStrategy());
    }

    private static String readProp(Path conf, String key, String def) throws IOException {
        for (String line : Files.readAllLines(conf)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int eq = line.indexOf('=');
            if (eq <= 0) continue;
            String k = line.substring(0, eq).trim();
            if (k.equals(key)) {
                return line.substring(eq + 1).trim();
            }
        }
        return def;
    }
}
