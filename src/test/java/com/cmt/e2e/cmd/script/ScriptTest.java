package com.cmt.e2e.cmd.script;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.xmlunit.assertj.XmlAssert.assertThat;

import com.cmt.e2e.support.CmtE2eTestBase;
import com.cmt.e2e.support.Scrubbers;
import com.cmt.e2e.support.annotation.TestResources;
import com.cmt.e2e.support.containers.CubridDemodbContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ScriptTest extends CmtE2eTestBase {

    @TempDir
    Path tempDir;

    @Test
    @TestResources("script")
    void cubridDemodb() throws IOException, InterruptedException {
        Path resourceDbConf = testPaths.getResourceDir().resolve("db.conf");
        CubridDemodbContainer.patchDbConf(resourceDbConf);

        workspaceFixtures.copyConfToWorkspace(resourceDbConf);

        String[] options = {"-s", "cubrid_source", "-t", "file_target", "-o", tempDir.toString()};
        runner.script(options);

        Path generatedScriptFile = testPaths.findGeneratedScriptFile(tempDir, "CUBRID_demodb_")
            .orElseThrow(() -> new AssertionError("Generated script file not found in " + tempDir));

        String actualXml = Files.readString(generatedScriptFile);
        String expectedXml = Files.readString(testPaths.getResourceDir().resolve("script.answer"));

        assertThat(Scrubbers.scrubXml(actualXml))
            .and(Scrubbers.scrubXml(expectedXml))
            .ignoreWhitespace().areSimilar();
    }
}
