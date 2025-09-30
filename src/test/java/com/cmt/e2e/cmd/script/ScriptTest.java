package com.cmt.e2e.cmd.script;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.cmt.e2e.support.Drivers.DB.CUBRID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.xmlunit.assertj.XmlAssert.assertThat;

import com.cmt.e2e.support.CmtE2eTestBase;
import com.cmt.e2e.support.Drivers;
import com.cmt.e2e.support.ProcessResult;
import com.cmt.e2e.support.Scrubbers;
import com.cmt.e2e.support.annotation.TestResources;
import com.cmt.e2e.support.containers.CubridDemodbContainer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ScriptTest extends CmtE2eTestBase {

    @TempDir
    Path tempDir;

    @Test
    @TestResources("script")
    void cubridDemodb() throws IOException, InterruptedException {
        Path resourceDbConf = testPaths.getResourceDir().resolve("db.conf");

        CubridDemodbContainer.patchDbConfHost(resourceDbConf, "cubrid_source");
        CubridDemodbContainer.patchDbConfPort(resourceDbConf, "cubrid_source");
        CubridDemodbContainer.patchDbConfDriver(resourceDbConf, "cubrid_source", Drivers.latest(CUBRID));
        CubridDemodbContainer.patchDbConfOutput(resourceDbConf, "file_target", testPaths.artifactDir);

        System.out.println("[RUNNER] workDir=" + cmtConsoleWorkDir.getPath());
        workspaceFixtures.copyConfToWorkspace(resourceDbConf);
        Path finalDbConf = cmtConsoleWorkDir.toPath().resolve("db.conf");
        System.out.println("--- FINAL db.conf ---");
        Files.lines(finalDbConf).forEach(System.out::println);
        System.out.println("---------------------");

        System.out.println("[NET] host.docker.internal = " + InetAddress.getByName("host.docker.internal").getHostAddress());
        try (var s = new Socket(CubridDemodbContainer.getHost(), CubridDemodbContainer.getMappedBrokerPort())) {
            System.out.println("[NET] TCP OK to demodb");
        } catch (Exception e) {
            throw new AssertionError("[NET] TCP FAIL to demodb: " +
                CubridDemodbContainer.getHost() + ":" + CubridDemodbContainer.getMappedBrokerPort(), e);
        }

        String[] options = {"-s", "cubrid_source", "-t", "file_target", "-o", tempDir.toString()};
        var result = runner.script(options);
        System.out.printf("[RUN-RESULT] exit=%d, timedOut=%s%n%s%n",
            result.exitCode(), result.timedOut(), result.output());
        assertEquals(0, result.exitCode(), "script failed");
        Files.list(tempDir).forEach(p -> System.out.println("[TEMP] " + p));

        Path generatedScriptFile = testPaths.findGeneratedScriptFile(tempDir, "CUBRID_demodb_")
            .orElseThrow(() -> new AssertionError("Generated script file not found in " + tempDir));

        String actualXml = Files.readString(generatedScriptFile);
        String expectedXml = Files.readString(testPaths.getResourceDir().resolve("script.answer"));

        assertThat(Scrubbers.scrubXml(actualXml))
            .and(Scrubbers.scrubXml(expectedXml))
            .ignoreWhitespace().areSimilar();
    }
}
