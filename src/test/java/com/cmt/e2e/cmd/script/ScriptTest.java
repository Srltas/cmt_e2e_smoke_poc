package com.cmt.e2e.cmd.script;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static com.cmt.e2e.support.Drivers.DB.CUBRID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.xmlunit.assertj.XmlAssert.assertThat;

import com.cmt.e2e.support.CmtE2eTestBase;
import com.cmt.e2e.support.Drivers;
import com.cmt.e2e.support.ProcessResult;
import com.cmt.e2e.support.Scrubbers;
import com.cmt.e2e.support.annotation.TestResources;
import com.cmt.e2e.support.containers.CubridDemodbContainer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ScriptTest extends CmtE2eTestBase {

    @TempDir
    Path tempDir;

    @Test
    @TestResources("script")
    void cubridDemodb() throws IOException, InterruptedException {
        Path resourceDbConf = testPaths.getResourceDir().resolve("db.conf");

        CubridDemodbContainer.patchDbConfHost(resourceDbConf, "cubrid_source");
        CubridDemodbContainer.patchDbConfPort(resourceDbConf, "cubrid_source");
        CubridDemodbContainer.patchDbConfDriver(resourceDbConf, "cubrid_source", Drivers.latest(CUBRID));
        CubridDemodbContainer.patchDbConfOutput(resourceDbConf, "file_target", testPaths.artifactDir);

        System.out.println("[RUNNER] workDir=" + cmtConsoleWorkDir.getPath());
        workspaceFixtures.copyConfToWorkspace(resourceDbConf);
        Path finalDbConf = cmtConsoleWorkDir.toPath().resolve("db.conf");

        String host = readProp(finalDbConf, "cubrid_source.host", "localhost");
        int port = Integer.parseInt(readProp(finalDbConf, "cubrid_source.port", "33000"));
        String dbname = readProp(finalDbConf, "cubrid_source.dbname", "demodb");
        String user = readProp(finalDbConf, "cubrid_source.user", "public");
        String pass = readProp(finalDbConf, "cubrid_source.password", "");
        String charset = readProp(finalDbConf, "cubrid_source.charset", "utf-8");

        System.out.println("--- FINAL db.conf ---");
        Files.lines(finalDbConf).forEach(System.out::println);
        System.out.println("---------------------");

        System.out.println("[NET] host.docker.internal = " + InetAddress.getByName("host.docker.internal").getHostAddress());
        try (var s = new Socket(CubridDemodbContainer.getHost(), CubridDemodbContainer.getMappedBrokerPort())) {
            System.out.println("[NET] TCP OK to demodb");
        } catch (Exception e) {
            throw new AssertionError("[NET] TCP FAIL to demodb: " +
                CubridDemodbContainer.getHost() + ":" + CubridDemodbContainer.getMappedBrokerPort(), e);
        }

        Path driverJar = Drivers.latest(CUBRID);
        jdbcPreflight(driverJar, host, port, dbname, user, pass, charset);

        String[] options = {"-s", "cubrid_source", "-t", "file_target", "-o", tempDir.toString()};
        var result = runner.script(options);
        System.out.printf("[RUN-RESULT] exit=%d, timedOut=%s%n%s%n",
            result.exitCode(), result.timedOut(), result.output());
        assertEquals(0, result.exitCode(), "script failed");
        Files.list(tempDir).forEach(p -> System.out.println("[TEMP] " + p));

        Path generatedScriptFile = testPaths.findGeneratedScriptFile(tempDir, "CUBRID_demodb_")
            .orElseThrow(() -> new AssertionError("Generated script file not found in " + tempDir));

        String actualXml = Files.readString(generatedScriptFile);
        String expectedXml = Files.readString(testPaths.getResourceDir().resolve("script.answer"));

        assertThat(Scrubbers.scrubXml(actualXml))
            .and(Scrubbers.scrubXml(expectedXml))
            .ignoreWhitespace().areSimilar();
    }

    static final class JdbcTry {
        final String label;
        final String url;
        final Properties props;

        public JdbcTry(String label, String url, Properties props) {
            this.label = label;
            this.url = url;
            this.props = props;
        }
    }

    private static void jdbcPreflight(Path driverJar, String host, int port, String dbname, String userInConf, String passInConf, String charset) {
        System.out.println("[JDBC-PREFLIGHT] jar=" + driverJar);
        System.out.println("[JDBC-PREFLIGHT] base cfg host=" + host + ", port=" + port + ", dbname=" + dbname +
            ", user=" + userInConf + ", pass=" + (passInConf == null ? "" : passInConf.replaceAll(".", "*")) +
            ", charset=" + charset);

        try (var cl = new URLClassLoader(new URL[]{driverJar.toUri().toURL()},
            Thread.currentThread().getContextClassLoader())) {
            Class<?> drvKlass = Class.forName("cubrid.jdbc.driver.CUBRIDDriver", true, cl);
            Driver drv = (Driver) drvKlass.getDeclaredConstructor().newInstance();

            String baseUrl = String.format("jdbc:cubrid:%s:%d:%s", host, port, dbname);

            List<JdbcTry> tries = new ArrayList<>();

            {
                String url = baseUrl + ":::?charset=" + charset;
                var p = new java.util.Properties();
                p.setProperty("user", userInConf == null ? "" : userInConf);
                p.setProperty("password", passInConf == null ? "" : passInConf);
                tries.add(new JdbcTry("A/conf-props", url, p));
            }

            {
                String url = baseUrl + ":::?charset=" + charset;
                var p = new java.util.Properties();
                p.setProperty("user", "dba");
                p.setProperty("password", "");
                tries.add(new JdbcTry("B/dba-props", url, p));
            }

            {
                String url = String.format("jdbc:cubrid:%s:%d:%s:%s:%s:?charset=%s",
                    host, port, dbname,
                    userInConf == null ? "" : userInConf,
                    passInConf == null ? "" : passInConf,
                    charset);
                tries.add(new JdbcTry("C/url-embed-user", url, new Properties()));
            }

            {
                String url = baseUrl + ":::?charset=" + charset + "&loginTimeout=10&keepConnection=true";
                var p = new Properties();
                p.setProperty("user", userInConf == null ? "" : userInConf);
                p.setProperty("password", passInConf == null ? "" : passInConf);
                tries.add(new JdbcTry("D/conf+timeout", url, p));
            }

            for (JdbcTry t : tries) {
                System.out.println("\n[JDBC-PREFLIGHT] TRY " + t.label);
                System.out.println("[JDBC-PREFLIGHT] URL=" + t.url);
                System.out.println("[JDBC-PREFLIGHT] PROPS=" + t.props);

                try (var conn = drv.connect(t.url, t.props)) {
                    if (conn == null) throw new RuntimeException("Driver returned null connection");
                    System.out.println("[JDBC-PREFLIGHT] OK " + t.label + " :: " +
                        conn.getMetaData().getDatabaseProductName() + " " +
                        conn.getMetaData().getDatabaseProductVersion());
                    return;
                } catch (SQLException se) {
                    System.out.println("[JDBC-PREFLIGHT] FAIL " + t.label);
                    System.out.println("  SQLException: SQLState=" + se.getSQLState() + ", errorCode=" + se.getErrorCode()
                        + ", msg=" + se.getMessage());
                    if (se.getCause() != null) System.out.println("  cause=" + se.getCause());
                } catch (Exception e) {
                    System.out.println("[JDBC-PREFLIGHT] FAIL " + t.label);
                    System.out.println("  CUBRIDException: " + e.getMessage());
                    try {
                        var getErrorCode = e.getClass().getMethod("getErrorCode");
                        Object ec = getErrorCode.invoke(e);
                        System.out.println("  getErrorCode=" + ec);
                    } catch (Throwable ignore) {}
                    if (e.getCause() != null) {
                        System.out.println("  cause=" + e.getCause());
                    }
                } catch (Throwable th) {
                    System.out.println("[JDBC-PREFLIGHT] FAIL " + t.label);
                    th.printStackTrace(System.out);
                }
            }
            throw new AssertionError("[JDBC-PREFLIGHT] All attempts failed (A~D)");
        } catch (Throwable t) {
            t.printStackTrace(System.out);
            throw new AssertionError("[JDBC] direct connect FAILED  (jar=" + driverJar + ")", t);

        }
    }

    private static String readProp(Path conf, String key, String def) throws IOException {
        for (String line : Files.readAllLines(conf)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int eq = line.indexOf('=');
            if (eq <= 0) continue;
            String k = line.substring(0, eq).trim();
            if (k.equals(key)) {
                return line.substring(eq + 1).trim();
            }
        }
        return def;
    }

    private static String mask(String s) {
        return s == null ? "" : s.replaceAll(".", "*");
    }
}
