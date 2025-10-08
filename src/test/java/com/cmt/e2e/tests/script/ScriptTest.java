package com.cmt.e2e.tests.script;

import com.cmt.e2e.command.CommandResult;
import com.cmt.e2e.command.impls.ScriptCommand;
import com.cmt.e2e.support.CmtE2eTestBase;
import com.cmt.e2e.support.annotation.TestResources;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ScriptTest extends CmtE2eTestBase {

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
        workspaceFixtures.setupCubridDemodbAsSource(testPaths);

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
