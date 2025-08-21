package com.cmt.e2e.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.assertj.core.util.diff.DiffUtils;
import org.assertj.core.util.diff.Patch;

public class AnswerAsserter {

    private final TestPaths paths;

    public AnswerAsserter(TestPaths paths) {
        this.paths = paths;
    }

    public String assertScriptXMLFile(String actualRaw, String expectedRaw) throws IOException {
        String actualScrubbed = Scrubbers.scrubXml(actualRaw);
        String expectedScrubbed = Scrubbers.scrubXml(expectedRaw);

        List<String> actualLines = Arrays.asList(actualScrubbed.split("\\R"));
        List<String> expectedLines = Arrays.asList(expectedScrubbed.split("\\R"));
        Patch<String> patch = DiffUtils.diff(expectedLines, actualLines);

        if (!patch.getDeltas().isEmpty()) {
            Path actualLogPath = paths.artifactDir.resolve("actual.log");
            Path expectedLogPath = paths.artifactDir.resolve("expected.log");
            Path diffPath = paths.artifactDir.resolve("diff.patch");

            Files.writeString(actualLogPath, actualRaw);
            Files.writeString(expectedLogPath, expectedRaw);

            List<String> diff = DiffUtils.generateUnifiedDiff(
                expectedLogPath.toString(), actualLogPath.toString(), expectedLines, patch, 3);
            Files.write(diffPath, diff, StandardCharsets.UTF_8);


            return String.join("\\R", diff);
        }
        return null;
    }

    public void assertAnswerFile(String actualRaw, String expectedFileName) throws IOException {
        Path expectFilePath = paths.resourceDir.resolve(expectedFileName);

        if (!Files.exists(expectFilePath)) {
            throw new AssertionError("Answer file not found at: " + expectFilePath);
        }

        String expectedRaw = Files.readString(expectFilePath);
        String actualScrubbed = Scrubbers.trimLines(actualRaw);
        String expectedScrubbed = Scrubbers.trimLines(expectedRaw);

        List<String> expectedLines = Arrays.asList(expectedScrubbed.split("\\R"));
        List<String> actualLines = Arrays.asList(actualScrubbed.split("\\R"));
        Patch<String> patch = DiffUtils.diff(expectedLines, actualLines);

        if (!patch.getDeltas().isEmpty()) {
            Path actualLogPath = paths.artifactDir.resolve("actual.log");
            Path expectedLogPath = paths.artifactDir.resolve("expected.log");
            Path diffPath = paths.artifactDir.resolve("diff.patch");

            Files.writeString(actualLogPath, actualRaw);
            Files.writeString(expectedLogPath, expectedRaw);

            List<String> diff = DiffUtils.generateUnifiedDiff(
                expectedLogPath.toString(), actualLogPath.toString(), expectedLines, patch, 3);
            Files.write(diffPath, diff, StandardCharsets.UTF_8);

            throw new AssertionError("Output does not match answer file. See diff: " + diffPath.toUri());
        }
    }

    private String truncate(String s, int max) {
        if (s.length() <= max) return s;
        return s.substring(0, max) + System.lineSeparator() + "...(diff truncated)";
    }
}
