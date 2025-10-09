package com.cmt.e2e.tests.error;

import com.cmt.e2e.framework.assertion.strategies.PlainTextVerificationStrategy;
import com.cmt.e2e.framework.command.Command;
import com.cmt.e2e.framework.command.CommandResult;
import com.cmt.e2e.framework.command.impls.RawCommand;
import com.cmt.e2e.framework.core.CmtE2eTestBase;
import com.cmt.e2e.framework.junit.annotation.TestResources;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class ErrorTest extends CmtE2eTestBase {

    private static final String UNKNOWN_COMMAND_ANSWER_FILENAME = "unknownCommand.answer";

    @Test
    @TestResources("error/unknownCommand")
    @DisplayName("알 수 없는 명령어를 입력하면 전체 도움말을 출력한다")
    void should_printErrorMessage_when_commandIsUnknown() throws IOException, InterruptedException {
        // Arrange
        Command unknownCommand = new RawCommand("./migration.sh", "unknown-command");

        // Act
        CommandResult result = commandRunner.run(unknownCommand);

        // Assert
        verifier.verifyWith(result, UNKNOWN_COMMAND_ANSWER_FILENAME, new PlainTextVerificationStrategy());
    }
}
