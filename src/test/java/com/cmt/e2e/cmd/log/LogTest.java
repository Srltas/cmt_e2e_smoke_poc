package com.cmt.e2e.cmd.log;

import java.nio.file.Path;

import com.cmt.e2e.support.CmtE2eTestBase;
import com.cmt.e2e.support.ProcessResult;
import com.cmt.e2e.support.annotation.TestResources;

public class LogTest extends CmtE2eTestBase {

    // Console을 System.in으로 변경해야 사용 가능
    @TestResources("log/ps_20")
    void testLogPaging() throws Exception {
        Path mhPath = testPaths.getResourceDir().resolve("1755661922744.mh");
        workspaceFixtures.copyResourceToWorkspace(mhPath);

        String[] options = {"-ps", "20", mhPath.getFileName().toString()};

        ProcessResult result = runner.log(options);

        answerAsserter.assertAnswerFile(result.output(), "expected.answer");
    }
}
