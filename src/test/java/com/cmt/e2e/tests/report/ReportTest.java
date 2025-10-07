package com.cmt.e2e.tests.report;

import java.io.IOException;

import static com.cmt.e2e.support.WorkspaceFixtures.CUBRID_DEMODB_MH;

import com.cmt.e2e.assertion.strategies.PlainTextVerificationStrategy;
import com.cmt.e2e.command.impls.ReportCommand;
import com.cmt.e2e.support.CmtE2eTestBase;
import com.cmt.e2e.command.CommandResult;
import com.cmt.e2e.support.annotation.CubridDemodbMh;
import com.cmt.e2e.support.annotation.TestResources;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ReportTest extends CmtE2eTestBase {

    @Test
    @CubridDemodbMh
    @TestResources("report/ao")
    @DisplayName("report 명령어 -ao 옵션 mh 지정")
    void report_ao() throws IOException, InterruptedException {
        ReportCommand command = ReportCommand.builder()
            .allOutput()
            .mhFile(CUBRID_DEMODB_MH)
            .build();

        CommandResult result = commandRunner.run(command);

        verifier.verifyWith(result, "report_ao.answer", new PlainTextVerificationStrategy());
    }

    @Test
    @CubridDemodbMh
    @TestResources("report/ao_l")
    @DisplayName("report 명령어 -ao, -l 옵션")
    void report_ao_l() throws IOException, InterruptedException {
        ReportCommand command = ReportCommand.builder()
            .allOutput()
            .latest()
            .mhFile(CUBRID_DEMODB_MH)
            .build();

        CommandResult result = commandRunner.run(command);

        verifier.verifyWith(result, "report_ao_l.answer", new PlainTextVerificationStrategy());
    }
}
