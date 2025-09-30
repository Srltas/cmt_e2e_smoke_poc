package com.cmt.e2e.cmd.script;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.cmt.e2e.support.Drivers.DB.CUBRID;
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

        System.out.println("[RUNNER] workDir=" + cmtConsoleWorkDir.getPath());
        workspaceFixtures.copyConfToWorkspace(resourceDbConf);
        Path finalDbConf = cmtConsoleWorkDir.toPath().resolve("db.conf");
        System.out.println("--- FINAL db.conf ---");
        Files.lines(finalDbConf).forEach(System.out::println);
        System.out.println("---------------------");

        String[] options = {"-s", "cubrid_source", "-t", "file_target", "-o", tempDir.toString()};
        var result = runner.script(options);

        Assertions.assertEquals(0, result.exitCode(), "migration.sh failed. exit=" + result.exitCode() + "\n--- OUTPUT ---\n" + result.output());
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
