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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import static com.cmt.e2e.support.Drivers.DB.CUBRID;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ScriptTest extends CmtE2eTestBase {
    private static final Logger log = LoggerFactory.getLogger(ScriptTest.class);

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
        // 1. DB 컨테이너 준비
        CubridDemodbContainer.waitUntilReady();

        // 2. 테스트용 db.conf 설정
        Path resourceDbConf = testPaths.getResourceDir().resolve(DB_CONF_FILENAME);
        CubridDemodbContainer.patchDbConfHost(resourceDbConf, CUBRID_SOURCE_NAME);
        CubridDemodbContainer.patchDbConfPort(resourceDbConf, CUBRID_SOURCE_NAME);
        CubridDemodbContainer.patchDbConfDriver(resourceDbConf, CUBRID_SOURCE_NAME, Drivers.latest(CUBRID));
        CubridDemodbContainer.patchDbConfOutput(resourceDbConf, FILE_TARGET_NAME, testPaths.artifactDir);

        // 3. 설정 파일을 실제 작업 공간에 복사
        workspaceFixtures.copyConfToWorkspace(resourceDbConf);

        // 4. 네트워크 및 JDBC 연결 사전 점검
        Path finalDbConf = cmtConsoleWorkDir.toPath().resolve(DB_CONF_FILENAME);
        String host = readProp(finalDbConf, CUBRID_SOURCE_NAME + ".host", "localhost");
        int port = Integer.parseInt(readProp(finalDbConf, CUBRID_SOURCE_NAME + ".port", "33000"));

        try (var s = new Socket(host, port)) {
            // 연결 성공 시 별도 로그 불필요
        } catch (Exception e) {
            throw new AssertionError("[NET] TCP preflight check FAILED to " + host + ":" + port, e);
        }

        String dbname = readProp(finalDbConf, CUBRID_SOURCE_NAME + ".dbname", "demodb");
        String user = readProp(finalDbConf, CUBRID_SOURCE_NAME + ".user", "public");
        String pass = readProp(finalDbConf, CUBRID_SOURCE_NAME + ".password", "");
        String charset = readProp(finalDbConf, CUBRID_SOURCE_NAME + ".charset", "utf-8");

        Path driverJar = Drivers.latest(CUBRID);
        JdbcPreflight.run(driverJar, new CubridJdbcUrlStrategy(), host, port, dbname, user, pass, charset);
    }

    private void verifyGeneratedScript(CommandResult result) throws IOException {
        // 1. 생성된 스크립트 파일을 찾습니다.
        Path generatedScriptFile = testPaths.findGeneratedScriptFile(tempDir, GENERATED_SCRIPT_PREFIX)
            .orElseThrow(() -> {
                // 파일을 못 찾았을 때만, 실패의 원인을 파악하기 위해 상세 디버그 로그를 남깁니다.
                String tempDirContents;
                try {
                    tempDirContents = Files.list(tempDir).map(Path::toString).collect(Collectors.joining("\n"));
                } catch (IOException e) {
                    tempDirContents = "Could not list temp directory:" + e.getMessage();
                }
                log.error("Generated script file not found in {}. Current contents:\n {}", tempDir, tempDirContents);
                return new AssertionError("Generated script file not found in " + tempDir);
            });

        log.debug("Found generated script file: {}", generatedScriptFile);

        // 2. 파일 내용을 읽어와서 실제 검증을 수행합니다.
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
