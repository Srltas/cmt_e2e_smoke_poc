package com.cmt.e2e.tests.report;

import com.cmt.e2e.framework.assertion.strategies.PlainTextVerificationStrategy;
import com.cmt.e2e.framework.command.CommandResult;
import com.cmt.e2e.framework.command.impls.ReportCommand;
import com.cmt.e2e.framework.core.CmtE2eTestBase;
import com.cmt.e2e.framework.junit.annotation.CubridDemodbMh;
import com.cmt.e2e.framework.junit.annotation.TestResources;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.cmt.e2e.framework.core.WorkspaceFixtures.CUBRID_DEMODB_MH;

public class ReportTest extends CmtE2eTestBase {

    private static final String REPORT_AO_ANSWER_FILENAME = "report_ao.answer";
    private static final String REPORT_AO_L_ANSWER_FILENAME = "report_ao_l.answer";

    @Test
    @CubridDemodbMh
    @TestResources("report/ao")
    @DisplayName("report 명령어에 -ao 옵션을 사용하면 전체 내용을 출력한다")
    void should_generateFullReport_when_allOutputOptionIsUsed() throws IOException, InterruptedException {
        // Arrange
        ReportCommand command = ReportCommand.builder()
            .allOutput()
            .mhFile(CUBRID_DEMODB_MH)
            .build();

        // Act
        CommandResult result = commandRunner.run(command);

        // Assert
        verifier.verifyWith(result, REPORT_AO_ANSWER_FILENAME, new PlainTextVerificationStrategy());
    }

    @Test
    @CubridDemodbMh
    @TestResources("report/ao_l")
    @DisplayName("report 명령어에 -ao와 -l 옵션을 함께 사용하면 가장 최신 작업의 전체 내용을 출력한다")
    void should_generateFullReportForLatest_when_allOutputAndLatestOptionsAreUsed() throws IOException, InterruptedException {
        // Arrange
        ReportCommand command = ReportCommand.builder()
            .allOutput()
            .latest()
            .mhFile(CUBRID_DEMODB_MH)
            .build();

        // Act
        CommandResult result = commandRunner.run(command);

        // Assert
        verifier.verifyWith(result, REPORT_AO_L_ANSWER_FILENAME, new PlainTextVerificationStrategy());
    }
}
