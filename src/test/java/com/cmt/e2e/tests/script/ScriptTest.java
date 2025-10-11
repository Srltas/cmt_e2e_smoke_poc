package com.cmt.e2e.tests.script;

import com.cmt.e2e.framework.command.CommandResult;
import com.cmt.e2e.framework.command.impls.ScriptCommand;
import com.cmt.e2e.framework.core.CmtE2eTestBase;
import com.cmt.e2e.framework.junit.annotation.TestResources;
import com.cmt.e2e.framework.db.config.OnlineToDumpConfig;
import com.cmt.e2e.framework.db.containers.CubridContainer;
import com.cmt.e2e.framework.db.jdbc.CubridJdbcUrlStrategy;
import com.cmt.e2e.framework.db.precond.TestPreconditions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.Container;

import java.nio.file.Path;

import static com.cmt.e2e.framework.db.driver.Drivers.DB.CUBRID;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ScriptTest extends CmtE2eTestBase {

    private static final String DB_CONF_FILENAME = "db.conf";
    private static final String SCRIPT_ANSWER_FILENAME = "script.answer";
    private static final String CUBRID_SOURCE_NAME = "cubrid_source";
    private static final String FILE_TARGET_NAME = "file_target";
    private static final String GENERATED_SCRIPT_PREFIX = "CUBRID_demodb_";

    @Container
    private final CubridContainer cubridDemodb = CubridContainer.withDemodb();

    @TempDir
    Path tempDir;

    @Test
    @TestResources("script")
    @DisplayName("온라인 CUBRID DB를 소스로 마이그레이션 스크립트를 생성한다")
    void should_generateMigrationScript_when_sourceIsCUBRIDOnline() throws Exception {
        // Arrange
        // 1. db.conf 템플릿으로부터 설정 객체를 생성하고, CUBRID demodb 정보로 내용을 채웁니다.
        Path confTemplate = testPaths.getResourceDir().resolve(DB_CONF_FILENAME);
        OnlineToDumpConfig dbConfig = OnlineToDumpConfig.fromTemplate(confTemplate, CUBRID_SOURCE_NAME, FILE_TARGET_NAME);
        dbConfig.patchWithContainer(cubridDemodb, CUBRID, testPaths.artifactDir);

        // 2. 최종 설정 파일을 작업 공간에 배치합니다.
        workspaceFixtures.copyConfToWorkspace(dbConfig.getFinalConfPath(), DB_CONF_FILENAME);

        // 3. DB 서버와의 통신 환경을 사전 점검합니다.
        TestPreconditions.assertTcpConnection(dbConfig);
        TestPreconditions.assertJdbcConnection(dbConfig, new CubridJdbcUrlStrategy());

        // 4. 실행할 Command를 생성합니다.
        ScriptCommand command = ScriptCommand.builder()
                .source(CUBRID_SOURCE_NAME)
                .target(FILE_TARGET_NAME)
                .output(tempDir.toString())
                .build();

        // Act
        CommandResult result = commandRunner.run(command);

        // Assert
        assertEquals(0, result.exitCode(), "Script command should succeed");
        verifier.verifyGeneratedScript(tempDir, GENERATED_SCRIPT_PREFIX, SCRIPT_ANSWER_FILENAME, result);
    }
}
