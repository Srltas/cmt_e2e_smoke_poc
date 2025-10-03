package com.cmt.e2e.support;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xmlunit.assertj.XmlAssert;
import org.xmlunit.diff.Comparison;
import org.xmlunit.diff.DifferenceEvaluator;
import org.xmlunit.diff.DifferenceEvaluators;
import org.xmlunit.util.Predicate;

import java.util.*;
import java.util.regex.Pattern;

import static org.xmlunit.diff.ComparisonResult.EQUAL;
import static org.xmlunit.diff.ComparisonType.ATTR_VALUE;
import static org.xmlunit.diff.ComparisonType.TEXT_VALUE;
import static org.xmlunit.diff.DifferenceEvaluators.Default;

/**
 * XML 비교 유틸.
 *
 * 목적
 * - CI/로컬 등 환경 차이로 바뀌는 속성값(@driver, @host, @port, @timezone, @dir...)은 "값 차이"만 무시
 * - 실행 시마다 달라지는 동적 값(타임스탬프 등)은 형식이 같으면 동일 취급
 *
 */
public final class XmlUtil {
    private XmlUtil() {}

    /** 환경 속성 + 동적 값 모두 처리하는 표준 비교 */
    public static void assertSimilarStandard(String actualXml, String expectedXml) {
        XmlAssert.assertThat(actualXml)
            .and(expectedXml)
            .ignoreWhitespace()
            .withDifferenceEvaluator(standardDifferenceEvaluator())
            .areSimilar();
    }

    /** 기본 규칙으로 “환경 속성의 값 차이”만 무시하여 비교(존재/부재는 검증) */
    public static void assertSimilarIgnoringEnv(String actualXml, String expectedXml) {
        XmlAssert.assertThat(actualXml)
            .and(expectedXml)
            .ignoreWhitespace()
            .withDifferenceEvaluator(differenceEvaluatorIgnoreAttrs(defaultIgnoredAttrsByOwner()))
            .areSimilar();
    }

    /** 기본 규칙으로 “해당 속성 자체”를 비교에서 제외(존재 여부도 무시) */
    public static void assertSimilarWithAttrFilter(String actualXml, String expectedXml) {
        XmlAssert.assertThat(actualXml)
            .and(expectedXml)
            .ignoreWhitespace()
            .withAttributeFilter(attributeFilterIgnoreAttrs(defaultIgnoredAttrsByOwner()))
            .areSimilar();
    }

    /**
     * 표준 DifferenceEvaluator:
     *  - Default → 환경 속성 값 차이 무시 → 동적 값 무시 순서로 체인
     *  - Default는 단 한 번만 적용
     */
    public static DifferenceEvaluator standardDifferenceEvaluator() {
        return DifferenceEvaluators.chain(
            Default,
            ignoreAttrsEvaluator(defaultIgnoredAttrsByOwner()),
            ignoreDynamicsEvaluator(defaultDynamicAttrPatterns(), defaultDynamicTextElements())
        );
    }

    /**
     * 환경 속성의 “값 차이”만 무시하는 DifferenceEvaluator.
     * 속성 존재/부재는 계속 검증됨.
     */
    public static DifferenceEvaluator differenceEvaluatorIgnoreAttrs(Map<String, Set<String>> byOwner) {
        return DifferenceEvaluators.chain(Default, ignoreAttrsEvaluator(
            byOwner == null ? defaultIgnoredAttrsByOwner() : byOwner
        ));
    }

    /**
     * 동적 값(타임스탬프 등)을 무시하는 DifferenceEvaluator.
     * - element@attr 값이 지정 패턴에 맞으면 값 차이 무시
     * - 특정 엘리먼트의 텍스트 노드는 값 차이 무시
     */
    public static DifferenceEvaluator differenceEvaluatorIgnoreDynamics(
        Map<String, Map<String, Pattern>> dynamicAttrPatterns,
        Set<String> dynamicTextElements
    ) {
        return DifferenceEvaluators.chain(Default, ignoreDynamicsEvaluator(
            dynamicAttrPatterns == null ? defaultDynamicAttrPatterns() : dynamicAttrPatterns,
            dynamicTextElements == null ? defaultDynamicTextElements() : dynamicTextElements
        ));
    }

    /**
     * 지정 속성들을 “비교 자체에서 제외”하는 AttributeFilter.
     * (속성 존재 여부까지 무시됨)
     */
    public static Predicate<Attr> attributeFilterIgnoreAttrs(Map<String, Set<String>> byOwner) {
        final Map<String, Set<String>> rules =
            byOwner == null ? defaultIgnoredAttrsByOwner() : byOwner;

        // XMLUnit Predicate: true → 비교 포함, false → 제외
        return attr -> !isIgnored(ownerName(attr), attrName(attr), rules);
    }

    /** 기본: 오너 엘리먼트 → 무시할 속성명 집합(환경 의존 값) */
    public static Map<String, Set<String>> defaultIgnoredAttrsByOwner() {
        return Map.of(
            "jdbc", Set.of("driver", "host", "port", "timezone", "user_jdbc_url"),
            "fileRepository", Set.of("dir", "timezone")
        );
    }

    /** 기본: 동적 속성 패턴(element@attr → 정규식) */
    public static Map<String, Map<String, Pattern>> defaultDynamicAttrPatterns() {
        Map<String, Map<String, Pattern>> m = new HashMap<>();
        putPattern(m, "migration", "name", Pattern.compile("CUBRID_demodb_\\d+"));
        putPattern(m, "migration", "wizardStartDateTime", Pattern.compile("\\d+"));
        return deepUnmodifiable(m);
    }

    /** 기본: 값이 매번 달라지는 텍스트 엘리먼트(내용 차이 무시) */
    public static Set<String> defaultDynamicTextElements() {
        return Set.of("creation_timestamp");
    }

    public static String normalizeForStandardDiff(String xml) {
        if (xml == null) return "";

        // 동적 속성(타임스탬프 등)
        String s = xml
            .replaceAll("name=\\\"CUBRID_demodb_\\d+\\\"", "name=\\\"CUBRID_demodb_...\\\"")
            .replaceAll("wizardStartDateTime=\\\"\\d+\\\"", "wizardStartDateTime=\\\"...\\\"")
            .replaceAll("(?s)<creation_timestamp>.+?</creation_timestamp>",
                "<creation_timestamp>REMOVED</creation_timestamp>");

        // 환경 의존 속성(비교 정책과 의미 일치)
        // jdbc 아래
        s = s.replaceAll("driver=\\\"[^\\\"]*?/([^/\\\"]+\\.jar)\\\"", "driver=\\\"$1\\\""); // 경로 → 파일명
        s = s.replaceAll("host=\\\"[^\\\"]+\\\"",     "host=\\\"<HOST>\\\"");
        s = s.replaceAll("port=\\\"\\d+\\\"",         "port=\\\"<PORT>\\\"");
        s = s.replaceAll("timezone=\\\"[^\\\"]+\\\"", "timezone=\\\"<TZ>\\\"");
        s = s.replaceAll("user_jdbc_url=\\\"[^\\\"]*\\\"", "user_jdbc_url=\\\"<URL>\\\"");

        // fileRepository 아래
        s = s.replaceAll("dir=\\\"[^\\\"]+\\\"",      "dir=\\\"<DIR>\\\"");
        // (이미 위 timezone 치환 규칙이 커버)

        return s;
    }

    /** 내부: 환경 속성의 “값 차이”만 무시(체인에 Default 포함 금지) */
    private static DifferenceEvaluator ignoreAttrsEvaluator(Map<String, Set<String>> rules) {
        return (comparison, outcome) -> {
            if (outcome == EQUAL) return outcome;
            if (comparison.getType() == ATTR_VALUE) {
                Attr attr = extractAttr(comparison);
                if (attr != null && isIgnored(ownerName(attr), attrName(attr), rules)) {
                    return EQUAL;
                }
            }
            return outcome;
        };
    }

    /** 내부: 동적 값 무시(체인에 Default 포함 금지) */
    private static DifferenceEvaluator ignoreDynamicsEvaluator(
        Map<String, Map<String, Pattern>> dynAttr,
        Set<String> dynTextElems
    ) {
        return (comparison, outcome) -> {
            if (outcome == EQUAL) return outcome;

            // 동적 "속성 값" 패턴
            if (comparison.getType() == ATTR_VALUE) {
                Attr a = extractAttr(comparison);
                if (a != null) {
                    Pattern p = dynAttr.getOrDefault(ownerName(a), Map.of()).get(attrName(a));
                    if (p != null) {
                        Object cv = comparison.getControlDetails().getValue();
                        Object tv = comparison.getTestDetails().getValue();
                        if (cv instanceof String && tv instanceof String
                            && p.matcher((String) cv).matches()
                            && p.matcher((String) tv).matches()) {
                            return EQUAL;
                        }
                    }
                }
            }

            // 동적 "텍스트 값"
            if (comparison.getType() == TEXT_VALUE) {
                Node target = firstNonNullTarget(comparison);
                String parent = parentElementName(target);
                if (parent != null && dynTextElems.contains(parent)) {
                    return EQUAL;
                }
            }

            return outcome;
        };
    }

    private static void putPattern(Map<String, Map<String, Pattern>> m, String owner, String attr, Pattern p) {
        m.computeIfAbsent(owner, k -> new HashMap<>()).put(attr, p);
    }

    /** 불변 Map<String, Map<String, Pattern>>로 변환 */
    private static Map<String, Map<String, Pattern>> deepUnmodifiable(Map<String, Map<String, Pattern>> src) {
        Map<String, Map<String, Pattern>> out = new HashMap<>();
        for (var e : src.entrySet()) {
            out.put(e.getKey(), Collections.unmodifiableMap(new HashMap<>(e.getValue())));
        }
        return Collections.unmodifiableMap(out);
    }

    /** rules에 의해 무시 대상인지(owner/attr 기준) */
    private static boolean isIgnored(String ownerElement, String attrName, Map<String, Set<String>> rules) {
        if (ownerElement == null || attrName == null) return false;
        Set<String> attrs = rules.get(ownerElement);
        return attrs != null && attrs.contains(attrName);
    }

    private static Attr extractAttr(Comparison c) {
        Node control = nodeOf(c.getControlDetails());
        Node test = nodeOf(c.getTestDetails());
        if (control instanceof Attr) return (Attr) control;
        if (test instanceof Attr) return (Attr) test;
        return null;
    }

    private static Node nodeOf(Comparison.Detail d) {
        return d == null ? null : d.getTarget();
    }

    private static String ownerName(Attr a) {
        return a.getOwnerElement() == null ? null : a.getOwnerElement().getNodeName();
    }

    private static String attrName(Attr a) {
        return a.getLocalName() != null ? a.getLocalName() : a.getName();
    }

    private static Node firstNonNullTarget(Comparison c) {
        Node n = nodeOf(c.getControlDetails());
        return n != null ? n : nodeOf(c.getTestDetails());
    }

    private static String parentElementName(Node n) {
        if (n == null) return null;
        Node p = n.getParentNode();
        return (p instanceof Element) ? ((Element) p).getNodeName() : null;
    }
}
