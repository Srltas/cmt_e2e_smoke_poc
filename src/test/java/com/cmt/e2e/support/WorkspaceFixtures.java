package com.cmt.e2e.support;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Optional;

import static com.cmt.e2e.support.Drivers.DB.CUBRID;

import com.cmt.e2e.support.annotation.CubridDemodbMh;
import com.cmt.e2e.support.containers.CubridDemodbContainer;
import com.cmt.e2e.support.jdbc.CubridJdbcUrlStrategy;
import com.cmt.e2e.support.jdbc.JdbcPreflight;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkspaceFixtures {
    private static final Logger log = LoggerFactory.getLogger(WorkspaceFixtures.class);

    public static final String CUBRID_DEMODB_MH = "1758883298370.mh";

    private final Path cmtConsoleDir;
    private final Path workspaceReportDir;
    private final TestInfo testInfo;

    /**
     * CMT 콘솔의 작업 디렉터리를 기반으로 report 디렉터리 경로를 설정
     * @param cmtConsoleWorkDir CMT_CONSOLE_HOME 경로
     */
    public WorkspaceFixtures(File cmtConsoleWorkDir, TestInfo testInfo) {
        this.cmtConsoleDir = cmtConsoleWorkDir.toPath();
        this.workspaceReportDir = cmtConsoleWorkDir.toPath().resolve("workspace/cmt/report");
        this.testInfo = testInfo;
    }

    public void copyConfToWorkspace(Path resourceConfPath) throws IOException {
        if (!Files.exists(resourceConfPath)) {
            throw new IOException("Resource file not found: " + resourceConfPath);
        }
        Path destinationPath = cmtConsoleDir.resolve(resourceConfPath.getFileName());
        log.debug("Copying config {} to {}", resourceConfPath, destinationPath);
        Files.copy(resourceConfPath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
    }

    public void cleanupConf() throws IOException {
        Path confPath = cmtConsoleDir.resolve("db.conf");
        Files.deleteIfExists(confPath);
    }

    public Optional<String> setupCubridDemodbMh() throws IOException {
        boolean isAnnotationPresent =
            testInfo.getTestMethod()
                .map(method -> method.isAnnotationPresent(CubridDemodbMh.class))
                .orElse(false)
            || testInfo.getTestClass()
                .map(clazz -> clazz.isAnnotationPresent(CubridDemodbMh.class))
                .orElse(false);

        if (isAnnotationPresent) {
            URL resourceUrl = getClass().getClassLoader().getResource("tests/mh/" + CUBRID_DEMODB_MH);
            if (resourceUrl == null) {
                throw new IOException("Shared mh file not found in resources/tests/mh: " + CUBRID_DEMODB_MH);
            }
            try {
                Path sourceMhPath = Paths.get(resourceUrl.toURI());
                copyResourceToWorkspace(sourceMhPath);
                return Optional.of(CUBRID_DEMODB_MH);
            } catch (URISyntaxException e) {
                throw new IOException("Failed to setup shared mh file", e);
            }
        }
        return Optional.empty();
    }

    /**
     * 테스트에 필요한 리소스(.mh 파일 등)를 report 작업 공간으로 복사
     * @param resourcePath
     * @return
     * @throws IOException
     */
    public Path copyResourceToWorkspace(Path resourcePath) throws IOException {
        if (!Files.exists(resourcePath)) {
            throw new IOException("Resource file not found: " + resourcePath);
        }

        Files.createDirectories(workspaceReportDir);

        Path destinationPath = workspaceReportDir.resolve(resourcePath.getFileName());
        Files.copy(resourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        return destinationPath;
    }

    public void cleanupWorkspace() throws IOException {
        if (Files.exists(workspaceReportDir)) {
            Files.walk(workspaceReportDir)
                .filter(path -> !path.equals(workspaceReportDir))
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }

    public void setupCubridDemodbAsSource(TestPaths testPaths) throws IOException, InterruptedException {
        // 1. DB 컨테이너 준비
        CubridDemodbContainer.waitUntilReady();

        final String dbConfFileName = "db.conf";
        final String cubridSourceName = "cubrid_source";
        final String fileTargetName = "file_target";

        // 2. 테스트용 db.conf 설정
        Path resourceDbConf = testPaths.getResourceDir().resolve(dbConfFileName);
        CubridDemodbContainer.patchDbConfHost(resourceDbConf, cubridSourceName);
        CubridDemodbContainer.patchDbConfPort(resourceDbConf, cubridSourceName);
        CubridDemodbContainer.patchDbConfDriver(resourceDbConf, cubridSourceName, Drivers.latest(CUBRID));
        CubridDemodbContainer.patchDbConfOutput(resourceDbConf, fileTargetName, testPaths.artifactDir);

        // 3. 설정 파일을 실제 작업 공간에 복사
        copyConfToWorkspace(resourceDbConf);

        // 4. 네트워크 및 JDBC 연결 사전 점검
        Path finalDbConf = cmtConsoleDir.resolve(dbConfFileName);
        String host = readProp(finalDbConf, cubridSourceName + ".host", "localhost");
        int port = Integer.parseInt(readProp(finalDbConf, cubridSourceName + ".port", "33000"));

        try (var s = new Socket(host, port)) {
            // 연결 성공 시 별도 로그 불필요
        } catch (Exception e) {
            throw new AssertionError("[NET] TCP preflight check FAILED to " + host + ":" + port, e);
        }

        String dbname = readProp(finalDbConf, cubridSourceName + ".dbname", "demodb");
        String user = readProp(finalDbConf, cubridSourceName + ".user", "public");
        String pass = readProp(finalDbConf, cubridSourceName + ".password", "");
        String charset = readProp(finalDbConf, cubridSourceName + ".charset", "utf-8");

        Path driverJar = Drivers.latest(CUBRID);
        JdbcPreflight.run(driverJar, new CubridJdbcUrlStrategy(), host, port, dbname, user, pass, charset);
    }

    private static String readProp(Path conf, String key, String def) throws IOException {
        for (String line : Files.readAllLines(conf)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int eq = line.indexOf('=');
            if (eq <= 0) continue;
            String k = line.substring(0, eq).trim();
            if (k.equals(key)) {
                return line.substring(eq + 1).trim();
            }
        }
        return def;
    }
}
