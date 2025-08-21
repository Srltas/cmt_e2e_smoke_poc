package com.cmt.e2e.cmd.report;

import java.io.IOException;

import static com.cmt.e2e.support.WorkspaceFixtures.CUBRID_DEMODB_MH;

import com.cmt.e2e.support.CmtE2eTestBase;
import com.cmt.e2e.support.ProcessResult;
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
        String[] options = {"-ao", CUBRID_DEMODB_MH};

        ProcessResult result = runner.report(options);

        answerAsserter.assertAnswerFile(result.output(), "report_ao.answer");
    }

    @Test
    @CubridDemodbMh
    @TestResources("report/ao_l")
    @DisplayName("report 명령어 -ao, -l 옵션")
    void report_ao_l() throws IOException, InterruptedException {
        String[] options = {"-ao", "-l", CUBRID_DEMODB_MH};

        ProcessResult result = runner.report(options);

        answerAsserter.assertAnswerFile(result.output(), "report_ao_l.answer");
    }
}
