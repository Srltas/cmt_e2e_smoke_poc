package com.cmt.e2e.cmd.log;

import static com.cmt.e2e.support.WorkspaceFixtures.CUBRID_DEMODB_MH;

import com.cmt.e2e.assertion.strategies.PlainTextVerificationStrategy;
import com.cmt.e2e.command.CommandResult;
import com.cmt.e2e.command.migration.LogCommand;
import com.cmt.e2e.support.CmtE2eTestBase;
import com.cmt.e2e.support.annotation.CubridDemodbMh;
import com.cmt.e2e.support.annotation.TestResources;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class LogTest extends CmtE2eTestBase {

    @Test
    @CubridDemodbMh
    @TestResources("log/ps_20")
    @DisplayName("log 명령어 -ps 옵션 mh 지정")
    void testLogPaging() throws Exception {
        LogCommand logCommand = LogCommand.builder()
            .pageSize(20)
            .mhFile(CUBRID_DEMODB_MH)
            .build();

        CommandResult result = commandRunner.run(logCommand);

        verifier.verifyWith(result, "expected.answer", new PlainTextVerificationStrategy());
    }
}
