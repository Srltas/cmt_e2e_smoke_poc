package com.cmt.e2e.tests.help;

import com.cmt.e2e.framework.assertion.strategies.PlainTextVerificationStrategy;
import com.cmt.e2e.framework.command.Command;
import com.cmt.e2e.framework.command.CommandResult;
import com.cmt.e2e.framework.command.impls.HelpCommand;
import com.cmt.e2e.framework.core.CmtE2eTestBase;
import com.cmt.e2e.framework.junit.annotation.TestResources;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class HelpTest extends CmtE2eTestBase {

    private static final String HELP_ANSWER_FILENAME = "commandHelp.answer";

    @Test
    @TestResources("help/command")
    @DisplayName("help 명령어를 실행하면 전체 도움말을 출력한다")
    void should_displayGeneralHelp_when_helpCommandIsExecuted() throws IOException, InterruptedException {
        // Arrange
        Command helpCommand = new HelpCommand();

        // Act
        CommandResult result = commandRunner.run(helpCommand);

        // Assert
        verifier.verifyWith(result, HELP_ANSWER_FILENAME, new PlainTextVerificationStrategy());
    }
}
