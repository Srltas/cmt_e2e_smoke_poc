package com.cmt.e2e.support;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Scrubbers {

    public static String trimLines(String input) {
        if (input == null) {
            return "";
        }
        return Arrays.stream(input.split("\\R"))
                .map(String::trim)
                .collect(Collectors.joining("\n"));
    }

    public static String scrubXml(String xmlContent) {
        if (xmlContent == null) {
            return "";
        }
        return xmlContent
            .replaceAll("name=\"CUBRID_demodb_\\d+\"", "name=\"CUBRID_demodb_...\"")
            .replaceAll("wizardStartDateTime=\"\\d+\"", "wizardStartDateTime=\"...\"")
            .replaceAll("(?s)<creation_timestamp>.+?</creation_timestamp>", "<creation_timestamp>REMOVED</creation_timestamp>");
    }
}
