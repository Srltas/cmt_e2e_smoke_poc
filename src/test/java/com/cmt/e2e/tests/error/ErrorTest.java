package com.cmt.e2e.tests.error;

import com.cmt.e2e.assertion.strategies.PlainTextVerificationStrategy;
import com.cmt.e2e.command.Command;
import com.cmt.e2e.command.CommandResult;
import com.cmt.e2e.command.impls.RawCommand;
import com.cmt.e2e.support.CmtE2eTestBase;
import com.cmt.e2e.support.annotation.TestResources;
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
