package com.cmt.e2e.framework.core;

import com.cmt.e2e.framework.assertion.Verifier;
import com.cmt.e2e.framework.command.CommandRunner;
import com.cmt.e2e.framework.junit.extension.FailureLogDumperExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
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
 * The base class for all E2E tests.
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

    /**
     * Sets up the test environment before each test method.
     * This method orchestrates the setup by calling purpose-specific helper methods.
     */
    @BeforeEach
    void setup(TestInfo testInfo) throws IOException {
        setupPathAndVerifier(testInfo);
        setupCommandRunner();
        setupTestFixtures(testInfo);
    }

    /**
     * Cleans up the test environment after each test method.
     */
    @AfterEach
    void cleanup() throws IOException {
        if (workspaceFixtures != null) {
            workspaceFixtures.cleanupWorkspace();
            workspaceFixtures.cleanupConf();
        }
    }

    /**
     * Initializes path-related and assertion-related components.
     */
    private void setupPathAndVerifier(TestInfo testInfo) throws IOException {
        this.testPaths = new TestPaths(testInfo);
        this.verifier = new Verifier(testPaths);
        log.debug("TestPaths and Verifier initialized for test: {}", testInfo.getDisplayName());
    }

    /**
     * Resolves the application's working directory and initializes the CommandRunner.
     */
    private void setupCommandRunner() throws IOException {
        String cmtConsoleHome = resolveCmtConsoleHome();
        assertThat(cmtConsoleHome)
            .withFailMessage("The CMT_CONSOLE_HOME environment variable must be set.")
            .isNotNull()
            .isNotEmpty();

        this.cmtConsoleWorkDir = new File(cmtConsoleHome);
        this.commandRunner = new CommandRunner(cmtConsoleWorkDir);
        log.debug("CommandRunner initialized with working directory: {}", cmtConsoleHome);
    }

    /**
     * Initializes and prepares the workspace fixtures for the current test.
     */
    private void setupTestFixtures(TestInfo testInfo) throws IOException {
        this.workspaceFixtures = new WorkspaceFixtures(cmtConsoleWorkDir, testInfo);
        this.workspaceFixtures.setupCubridDemodbMh();
        log.debug("WorkspaceFixtures initialized.");
    }

    /**
     * Resolves the CMT console home directory path.
     * It prioritizes the environment variable over the properties file.
     *
     * @return The resolved path to the CMT console home directory.
     * @throws IOException if reading the properties file fails.
     */
    private String resolveCmtConsoleHome() throws IOException {
        String homePath = System.getenv("CMT_CONSOLE_HOME");
        if (homePath != null && !homePath.isBlank()) {
            log.debug("Using CMT_CONSOLE_HOME from environment variable: {}", homePath);
            return homePath;
        }

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
