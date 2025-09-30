package com.cmt.e2e.support.containers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

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

    /** host를 동적으로 주입 */
    public static void patchDbConfHost(Path conf, String prefix) throws IOException {
        ensureUp();
        upsertProperty(conf, prefix + ".host", getHost());
    }

    /** port를 동적으로 주입 */
    public static void patchDbConfPort(Path conf, String prefix) throws IOException {
        ensureUp();
        upsertProperty(conf, prefix + ".port", String.valueOf(getMappedBrokerPort()));
    }

    /** driver 절대경로를 주입 */
    public static void patchDbConfDriver(Path conf, String prefix, Path driverJar) throws IOException {
        upsertProperty(conf, prefix + ".driver", driverJar.toAbsolutePath().toString());
    }

    /** file output 절대경로 주입 */
    public static void patchDbConfOutput(Path conf, String prefix, Path outDir) throws IOException {
        Files.createDirectories(outDir);
        upsertProperty(conf, prefix + ".output", outDir.toAbsolutePath().resolve("output").toString());
    }

    private static void upsertProperty(Path conf, String key, String value) throws IOException {
        if (!Files.exists(conf)) {
            throw new FileNotFoundException("db.conf not found: " + conf);
        }
        List<String> lines = Files.readAllLines(conf, StandardCharsets.UTF_8);
        boolean replaced = false;
        Pattern p = Pattern.compile("^\\s*" + Pattern.quote(key) + "\\s*=.*$");

        for (int i = 0; i < lines.size(); i++) {
            if (p.matcher(lines.get(i)).matches()) {
                lines.set(i, key + "=" + value);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            lines.add(key + "=" + value);
        }
        Files.write(conf, lines, StandardCharsets.UTF_8);
        System.out.println("[DBCONF] " + key + "=" + value);
    }
}
