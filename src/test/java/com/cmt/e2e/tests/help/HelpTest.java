package com.cmt.e2e.tests.help;

import java.io.IOException;

import com.cmt.e2e.assertion.strategies.PlainTextVerificationStrategy;
import com.cmt.e2e.command.Command;
import com.cmt.e2e.command.CommandResult;
import com.cmt.e2e.command.impls.HelpCommand;
import com.cmt.e2e.support.CmtE2eTestBase;
import com.cmt.e2e.support.annotation.TestResources;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class HelpTest extends CmtE2eTestBase {

    @Test
    @TestResources("help/command")
    @DisplayName("명령어 help 확인")
    void commandHelp() throws IOException, InterruptedException {
        Command helpCommand = new HelpCommand();

        CommandResult result = commandRunner.run(helpCommand);

        verifier.verifyWith(result, "commandHelp.answer", new PlainTextVerificationStrategy());
    }
}
