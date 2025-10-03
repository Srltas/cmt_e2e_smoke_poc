package com.cmt.e2e.cmd.error;

import java.io.IOException;

import com.cmt.e2e.support.CmtE2eTestBase;
import com.cmt.e2e.support.ProcessResult;
import com.cmt.e2e.support.annotation.TestResources;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ErrorTest extends CmtE2eTestBase {

    @Test
    @TestResources("error/unknownCommand")
    @DisplayName("유효하지 않는 명령어 입력")
    void unknownCommand() throws IOException, InterruptedException {
        // 1. Arrange
        String[] command = {"./migration.sh", "unknown-command"};

        // 2. Act
        ProcessResult result = runner.run(command);

        // 3. Assert
        answerAsserter.assertTextWithAnswerFile(result.output(), "unknownCommand.answer");
    }
}
