package com.cmt.e2e.support.containers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CubridDemodbContainer {

    private static final DockerImageName IMAGE = DockerImageName.parse("cubrid/cubrid:11.4");

    @Container
    public static final GenericContainer<?> CUBRID =
        new GenericContainer<>(IMAGE)
            .withExposedPorts(33000)
            .withCommand("bash", "-lc",
                "set -eux;" + "cubrid service start;" +
                "cubrid server start demodb;" +
                "cubrid broker start;" +
                "tail -f /dev/null")
            .waitingFor(Wait.forListeningPort())
            .withStartupTimeout(Duration.ofMinutes(2));

    public static synchronized void ensureUp() {
        if (!CUBRID.isRunning()) {
            CUBRID.start();
        }
    }

    public static GenericContainer<?> getDemodb() {
        ensureUp();
        return CUBRID;
    }

    public static String getHost() {
        ensureUp();
        return CUBRID.getHost();
    }

    public static int getMappedBrokerPort() {
        ensureUp();
        return CUBRID.getMappedPort(33000);
    }

    public static void patchDbConf(Path conf) throws IOException {
        ensureUp();
        String host = getHost();
        int port = getMappedBrokerPort();

        if (!Files.exists(conf)) {
            throw new IllegalStateException("db.conf not found: " + conf);
        }

        List<String> lines = Files.readAllLines(conf, StandardCharsets.UTF_8);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.matches("^\\s*cubrid_source\\.host\\s*=.*$")) {
                lines.set(i, "cubrid_source.host=" + host);
            } else if (line.matches("^\\s*cubrid_source\\.port\\s*=.*$")) {
                lines.set(i, "cubrid_source.port=" + port);
            }
        }

        Files.write(conf, lines, StandardCharsets.UTF_8);
        System.out.println("Patched cubrid_source.host=" + host + ", cubrid_source.port=" + port);
    }
}
