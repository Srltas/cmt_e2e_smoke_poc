package com.cmt.e2e.assertion.strategies;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.cmt.e2e.command.CommandResult;
import com.cmt.e2e.support.XmlUtil;

public class XmlVerificationStrategy implements VerificationStrategy {

    @Override
    public void verify(CommandResult actualResult, Path expectedAnswerPath) throws IOException {
        String expectedXml = Files.readString(expectedAnswerPath);
        String actualXml = findActualXml(actualResult);

        XmlUtil.assertSimilarStandard(actualXml, expectedXml);
    }

    private String findActualXml(CommandResult actualResult) {
        return actualResult.output();
    }
}
