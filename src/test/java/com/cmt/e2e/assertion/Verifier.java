package com.cmt.e2e.assertion;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.cmt.e2e.assertion.strategies.VerificationStrategy;
import com.cmt.e2e.command.CommandResult;
import com.cmt.e2e.support.TestPaths;
import org.assertj.core.util.diff.DiffUtils;
import org.assertj.core.util.diff.Patch;

/**
 * 테스트 검증을 수행하고, 실패 시 상세 리포트(아티팩트)를 생성하는 책임을 가집니다.
 */
public class Verifier {

    private final TestPaths testPaths;

    public Verifier(TestPaths testPaths) {
        this.testPaths = testPaths;
    }

    public void verifyWith(CommandResult result, String expectedAnswerFileName, VerificationStrategy strategy) throws IOException {
        Path expectedAnswerPath = testPaths.getResourceDir().resolve(expectedAnswerFileName);
        if (!Files.exists(expectedAnswerPath)) {
            throw new AssertionError("Answer file not found: " + expectedAnswerPath);
        }

        try {
            strategy.verify(result, expectedAnswerPath);
        } catch (AssertionError e) {
            String expectedContent = Files.readString(expectedAnswerPath);
            writeFailureArtifacts(result.output(), expectedContent, e);
            throw new AssertionError("Verification failed. See diff artifact for details: " +
                testPaths.artifactDir.resolve("diff.patch").toUri(), e);
        }
    }

    private void writeFailureArtifacts(String actualContent, String expectedContent, AssertionError rootError) throws IOException {
        Files.createDirectories(testPaths.artifactDir);

        List<String> actualLines = Arrays.asList(actualContent.split("\\R"));
        List<String> expectedLines = Arrays.asList(expectedContent.split("\\R"));
        Patch<String> patch = DiffUtils.diff(expectedLines, actualLines);
        List<String> diff = DiffUtils.generateUnifiedDiff("expected.log", "actual.log", expectedLines, patch, 3);

        Path actualPath = testPaths.artifactDir.resolve("actual.log");
        Path expectedPath = testPaths.artifactDir.resolve("expected.log");
        Path diffPath = testPaths.artifactDir.resolve("diff.patch");

        Files.writeString(actualPath, actualContent, UTF_8);
        Files.writeString(expectedPath, expectedContent, UTF_8);
        Files.write(diffPath, diff, UTF_8);
    }
}
