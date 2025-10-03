package com.cmt.e2e.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static com.cmt.e2e.support.XmlUtil.normalizeForStandardDiff;
import static java.nio.charset.StandardCharsets.UTF_8;

import org.assertj.core.util.diff.DiffUtils;
import org.assertj.core.util.diff.Patch;

public class AnswerAsserter {

    private final TestPaths paths;

    public AnswerAsserter(TestPaths paths) {
        this.paths = paths;
    }

    public void assertScriptXmlWithAnswerFile(String actualXml, String expectedResourceName) throws IOException {
        Path expectedPath = paths.resourceDir.resolve(expectedResourceName);
        if (!Files.exists(expectedPath)) {
            throw new AssertionError("Answer file not found: " + expectedPath);
        }
        String expectedXml = Files.readString(expectedPath);
        assertScriptXml(actualXml, expectedXml);
    }

    public void assertScriptXml(String actualXml, String expectedXml) throws IOException {
        try {
            XmlUtil.assertSimilarStandard(actualXml, expectedXml);
        } catch (AssertionError e) {
            writeXmlArtifactsAndThrow(actualXml, expectedXml, e);
        }
    }

    public void assertTextWithAnswerFile(String actualText, String expectedResourceName) throws IOException {
        Path expectedPath = paths.resourceDir.resolve(expectedResourceName);
        if (!Files.exists(expectedPath)) {
            throw new AssertionError("Answer file not found: " + expectedPath);
        }
        String expectedText = Files.readString(expectedPath);
        assertText(actualText, expectedText);
    }

    public void assertText(String actualText, String expectedText) throws IOException {
        String actual = trimLines(actualText);
        String expected = trimLines(expectedText);
        if (!actual.equals(expected)) {
            writeTextArtifactsAndThrow(actualText, expectedText);
        }
    }

    private void writeXmlArtifactsAndThrow(String actualXml, String expectedXml, AssertionError root) throws IOException {
        Files.createDirectories(paths.artifactDir);

        String actualNorm = normalizeForStandardDiff(actualXml);
        String expectedNorm = normalizeForStandardDiff(expectedXml);

        List<String> actualLines = Arrays.asList(actualNorm.split("\\R"));
        List<String> expectedLines = Arrays.asList(expectedNorm.split("\\R"));
        Patch<String> patch = DiffUtils.diff(expectedLines, actualLines);
        List<String> diff = DiffUtils.generateUnifiedDiff("expected.xml", "actual.xml", expectedLines, patch, 3);

        Path actualPath = paths.artifactDir.resolve("actual.log");
        Path expectedPath = paths.artifactDir.resolve("expected.log");
        Path diffPath = paths.artifactDir.resolve("diff.patch");

        Files.writeString(actualPath, actualXml, UTF_8);
        Files.writeString(expectedPath, expectedXml, UTF_8);
        Files.write(diffPath, diff, UTF_8);

        throw new AssertionError("XML mismatch. See diff: " + diffPath.toUri(), root);
    }

    private void writeTextArtifactsAndThrow(String actualText, String expectedText) throws IOException {
        Files.createDirectories(paths.artifactDir);

        List<String> actualLines = Arrays.asList(trimLines(actualText).split("\\R"));
        List<String> expectedLines = Arrays.asList(trimLines(expectedText).split("\\R"));
        Patch<String> patch = DiffUtils.diff(expectedLines, actualLines);
        List<String> diff = DiffUtils.generateUnifiedDiff("expected.log", "actual.log", expectedLines, patch, 3);

        Path actualPath = paths.artifactDir.resolve("actual.log");
        Path expectedPath = paths.artifactDir.resolve("expected.log");
        Path diffPath = paths.artifactDir.resolve("diff.patch");
        Files.writeString(actualPath, actualText, UTF_8);
        Files.writeString(expectedPath, expectedText, UTF_8);
        Files.write(diffPath, diff, UTF_8);

        throw new AssertionError("Text mismatch. See diff: " + diffPath.toUri());
    }

    private static String trimLines(String input) {
        if (input == null) return "";
        String[] lines = input.split("\\R");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) sb.append('\n');
            sb.append(lines[i].trim());
        }
        return sb.toString();
    }
}
