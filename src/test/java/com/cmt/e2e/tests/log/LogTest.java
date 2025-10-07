package com.cmt.e2e.tests.log;

import static com.cmt.e2e.support.WorkspaceFixtures.CUBRID_DEMODB_MH;

import com.cmt.e2e.assertion.strategies.PlainTextVerificationStrategy;
import com.cmt.e2e.command.CommandResult;
import com.cmt.e2e.command.impls.LogCommand;
import com.cmt.e2e.support.CmtE2eTestBase;
import com.cmt.e2e.support.annotation.CubridDemodbMh;
import com.cmt.e2e.support.annotation.TestResources;
import org.junit.jupiter.api.DisplayName;

public class LogTest extends CmtE2eTestBase {

    // System.console로 엔터 입력을 받고 있어 현재 테스트 자동화 불가능
    //TODO: System.in으로 변경되면 테스트 코드 수정 필요
    @CubridDemodbMh
    @TestResources("log/ps_20")
    @DisplayName("log 명령어 첫 페이지 출력 검증")
    void testLogPaging() throws Exception {
        LogCommand logCommand = LogCommand.builder()
            .pageSize(20)
            .mhFile(CUBRID_DEMODB_MH)
            .build();

        CommandResult result = commandRunner.run(logCommand);

        verifier.verifyWith(result, "expected.answer", new PlainTextVerificationStrategy());
    }
}
