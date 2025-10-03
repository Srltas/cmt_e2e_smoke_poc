package com.cmt.e2e.cmd.help;

import java.io.IOException;

import com.cmt.e2e.support.CmtE2eTestBase;
import com.cmt.e2e.support.ProcessResult;
import com.cmt.e2e.support.annotation.TestResources;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class HelpTest extends CmtE2eTestBase {

    @Test
    @TestResources("help/command")
    @DisplayName("명령어 help 확인")
    void commandHelp() throws IOException, InterruptedException {
        String[] command = {"./migration.sh"};

        ProcessResult result = runner.run(command);

        answerAsserter.assertTextWithAnswerFile(result.output(), "commandHelp.answer");
    }
}
