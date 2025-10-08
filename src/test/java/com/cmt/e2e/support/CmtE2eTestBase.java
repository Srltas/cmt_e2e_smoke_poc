package com.cmt.e2e.support;

import com.cmt.e2e.assertion.Verifier;
import com.cmt.e2e.runner.CommandRunner;
import com.cmt.e2e.support.extension.FailureLogDumperExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 모든 E2E 테스트가 상속받는 기본 클래스
 * 골든 파일 비교, 테스트별 작업 디렉터리 생성 등 공통 기능 제공
 */
@Testcontainers
public abstract class CmtE2eTestBase {
    private static final Logger log = LoggerFactory.getLogger(CmtE2eTestBase.class);

    @RegisterExtension
    final FailureLogDumperExtension failureLogDumper = new FailureLogDumperExtension();

    protected TestPaths testPaths;
    protected Verifier verifier;
    protected File cmtConsoleWorkDir;
    protected CommandRunner commandRunner;
    protected WorkspaceFixtures workspaceFixtures;

    @BeforeEach
    void setup(TestInfo testInfo) throws IOException {
        this.testPaths = new TestPaths(testInfo);
        this.verifier = new Verifier(testPaths);

        String cmtConsoleHome = resolveCmtConsoleHome();

        assertThat(cmtConsoleHome).withFailMessage("CMT_CONSOLE_HOME 환경 변수가 설정되지 않았습니다.")
            .isNotNull()
            .isNotEmpty();
        this.cmtConsoleWorkDir = new File(cmtConsoleHome);
        this.commandRunner = new CommandRunner(this.cmtConsoleWorkDir);

        this.workspaceFixtures = new WorkspaceFixtures(this.cmtConsoleWorkDir, testInfo);
        // @CubridDemodbMh 어노테이션이 있다면 .mh 파일을 자동으로 준비
        this.workspaceFixtures.setupCubridDemodbMh();
    }

    @AfterEach
    void cleanup() throws IOException {
        this.workspaceFixtures.cleanupWorkspace();
        this.workspaceFixtures.cleanupConf();
    }

    private String resolveCmtConsoleHome() throws IOException {
        // 1. 환경 변수 우선 확인
        String homePath = System.getenv("CMT_CONSOLE_HOME");
        if (homePath != null && !homePath.isBlank()) {
            log.debug("Using CMT_CONSOLE_HOME from environment variable: {}", homePath);
            return homePath;
        }

        // 2. e2e-test.properties 파일 확인
        Path propsPath = Paths.get("e2e-test.properties");
        if (Files.exists(propsPath)) {
            Properties props = new Properties();
            try (InputStream input = Files.newInputStream(propsPath)) {
                props.load(input);
                homePath = props.getProperty("cmt.console.home");
                if (homePath != null && !homePath.isBlank()) {
                    log.debug("Using cmt.console.home form e2e-test.properties: {}", homePath);
                    return homePath;
                }
            }
        }
        return null;
    }
}
