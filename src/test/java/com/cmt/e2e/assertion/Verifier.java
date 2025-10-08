package com.cmt.e2e.assertion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.cmt.e2e.assertion.strategies.VerificationStrategy;
import com.cmt.e2e.assertion.strategies.XmlVerificationStrategy;
import com.cmt.e2e.command.CommandResult;
import com.cmt.e2e.support.TestPaths;
import org.assertj.core.util.diff.DiffUtils;
import org.assertj.core.util.diff.Patch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 테스트 검증을 수행하고, 실패 시 상세 리포트(아티팩트)를 생성하는 책임을 가집니다.
 */
public class Verifier {
    private static final Logger log = LoggerFactory.getLogger(Verifier.class);

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

    public void verifyGeneratedScript(Path outputDir, String scriptPrefix, String expectedAnswerFileName, CommandResult originalCommandResult)
        throws IOException {
        // 1. 생성된 스크립트 파일을 찾습니다.
        Path generatedScriptFile = testPaths.findGeneratedScriptFile(outputDir, scriptPrefix)
            .orElseThrow(() -> {
                // 파일을 못 찾았을 때만, 실패의 원인을 파악하기 위해 상세 디버그 로그를 남깁니다.
                String tempDirContents;
                try {
                    tempDirContents = Files.list(outputDir).map(Path::toString).collect(Collectors.joining("\n"));
                } catch (IOException e) {
                    tempDirContents = "Could not list temp directory:" + e.getMessage();
                }
                log.error("Generated script file not found in {}. Current contents:\n {}", outputDir, tempDirContents);
                return new AssertionError("Generated script file not found in " + outputDir);
            });

        log.debug("Found generated script file: {}", generatedScriptFile);

        // 2. 파일 내용을 읽어와서 실제 검증을 수행합니다.
        String actualXml = Files.readString(generatedScriptFile);
        CommandResult verificationResult = new CommandResult(actualXml, originalCommandResult.exitCode(), originalCommandResult.timedOut());
        verifyWith(verificationResult, expectedAnswerFileName, new XmlVerificationStrategy());
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
