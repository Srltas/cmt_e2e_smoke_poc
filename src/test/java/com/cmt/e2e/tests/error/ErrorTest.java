package com.cmt.e2e.tests.error;

import java.io.IOException;

import com.cmt.e2e.assertion.strategies.PlainTextVerificationStrategy;
import com.cmt.e2e.command.Command;
import com.cmt.e2e.command.CommandResult;
import com.cmt.e2e.command.impls.RawCommand;
import com.cmt.e2e.support.CmtE2eTestBase;
import com.cmt.e2e.support.annotation.TestResources;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ErrorTest extends CmtE2eTestBase {

    @Test
    @TestResources("error/unknownCommand")
    @DisplayName("유효하지 않는 명령어 입력")
    void unknownCommand() throws IOException, InterruptedException {
        // 1. Arrange
        Command unknownCommand = new RawCommand("./migration.sh", "unknown-command");

        // 2. Act
        CommandResult result = commandRunner.run(unknownCommand);

        // 3. Assert
        verifier.verifyWith(result, "unknownCommand.answer", new PlainTextVerificationStrategy());
    }
}
