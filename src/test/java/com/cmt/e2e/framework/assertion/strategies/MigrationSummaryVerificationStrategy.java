package com.cmt.e2e.framework.assertion.strategies;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.cmt.e2e.framework.assertion.VerificationFailedException;
import com.cmt.e2e.framework.command.CommandResult;

public class MigrationSummaryVerificationStrategy implements VerificationStrategy{

    @Override
    public void verify(CommandResult result, Path expectedAnswerPath) throws IOException, AssertionError {
        String actualResult = trimLines(sanitize(result.output()));
        String expectedResult = trimLines(Files.readString(expectedAnswerPath));

        if (!actualResult.equals(expectedResult)) {
            throw new VerificationFailedException("Sanitized text content does not math.", actualResult, expectedResult);
        }
    }

    private String sanitize(String output) {
        if (output == null) return "";

        String normalized = output.replace("\r\n", "\n").replace("\r", "\n");

        List<String> keptLines = new ArrayList<>();
        boolean inProgressSection = false;

        for (String line : normalized.split("\n")) {
            if (!inProgressSection) {
                keptLines.add(line);
                if (line.contains("Migration started at")) {
                    inProgressSection = true;
                }
            } else {
                if (line.startsWith("-------------------------------------------------------------")) {
                    keptLines.add(line);
                    inProgressSection = false;
                }
            }
        }

        return String.join("\n", keptLines)
            .replaceAll("(?m)^Migration started at .*", "Migration started at [TIMESTAMP]")
            .replaceAll("(?m)^Reading <.*>", "Reading <[SCRIPT_PATH]>")
            .replaceAll("(?m)^\\s+Time used: .*", "    Time used: [DURATION]");
    }

    private String trimLines(String input) {
        if (input == null) return "";
        return Arrays.stream(input.split("\\R"))
            .map(String::trim)
            .collect(Collectors.joining("\n"));
    }
}
