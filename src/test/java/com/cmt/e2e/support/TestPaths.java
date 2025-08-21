package com.cmt.e2e.support;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

import com.cmt.e2e.support.annotation.TestResources;
import org.junit.jupiter.api.TestInfo;

public class TestPaths {

    public final Path resourceDir;
    public final Path artifactDir;

    public TestPaths(TestInfo testInfo) throws IOException {
        String testClassName = testInfo.getTestClass().map(Class::getSimpleName).orElse("UnknownClass");
        String testMethodName = testInfo.getTestMethod().map(Method::getName).orElse("UnknownMethod");

        Optional<TestResources> annotation = findTestResourcesAnnotation(testInfo);
        if (annotation.isEmpty()) {
            throw new IllegalStateException("Test class or method must have @TestResources annotation");
        }
        String resourcePath = "tests/" + annotation.get().value();
        this.resourceDir = getPathFromResources(resourcePath);

        this.artifactDir = Paths.get("target", "e2e", testClassName, testMethodName);
        Files.createDirectories(this.artifactDir);
    }

    /**
     * @TestResources 어노테이션으로 계산된 리소스 디렉터리 경로를 반환합니다.
     * @return 예: /path/to/project/target/test-classes/tests/log/ps_paging/basic
     */
    public Path getResourceDir() {
        return resourceDir;
    }

    public Optional<Path> findGeneratedScriptFile(Path directory, String fileName) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(path -> path.getFileName().toString().startsWith(fileName))
                .findFirst();
        }
    }

    private Optional<TestResources> findTestResourcesAnnotation(TestInfo testInfo) {
        return testInfo.getTestMethod()
            .map(method -> method.getAnnotation(TestResources.class))
            .or(() -> testInfo.getTestClass().map(clazz -> clazz.getAnnotation(TestResources.class)));
    }

    private Path getPathFromResources(String path) {
        try {
            URL url = getClass().getClassLoader().getResource(path);
            if (url == null) return Paths.get("src", "test", "resources", path);
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to get resource path", e);
        }
    }
}
