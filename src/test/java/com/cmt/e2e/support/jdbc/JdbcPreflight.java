package com.cmt.e2e.support.jdbc;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.List;

import com.cmt.e2e.support.TestLogHolder;

public final class JdbcPreflight {
    private JdbcPreflight() {}

    public static void run(Path driverJar, JdbcUrlStrategy strategy,
                           String host, int port, String dbname, String user, String pass, String charset) {
        TestLogHolder.log("[JDBC-PREFLIGHT] jar=" + driverJar);
        TestLogHolder.log("[JDBC-PREFLIGHT] base cfg host=%s, port=%d, dbname=%s, user=%s, pass=%s, charset=%s",
            host, port, dbname, user, mask(pass), charset);

        try (var cl = new URLClassLoader(new URL[]{driverJar.toUri().toURL()},
            Thread.currentThread().getContextClassLoader())) {
            Class<?> drvKlass = Class.forName(strategy.getDriverClassName(), true, cl);
            Driver drv = (Driver) drvKlass.getDeclaredConstructor().newInstance();

            List<JdbcConnectionTry> tries = strategy.buildConnectionAttempts(host, port, dbname, user, pass, charset);

            for (JdbcConnectionTry t : tries) {
                TestLogHolder.log("\n[JDBC-PREFLIGHT] TRY " + t.label());
                TestLogHolder.log("[JDBC-PREFLIGHT] URL=" + t.url());
                TestLogHolder.log("[JDBC-PREFLIGHT] PROPS=" + t.props());

                try (var conn = drv.connect(t.url(), t.props())) {
                    if (conn == null) throw new RuntimeException("Driver returned null connection");
                    TestLogHolder.log("[JDBC-PREFLIGHT] OK %s :: %s %s", t.label(),
                        conn.getMetaData().getDatabaseProductName(),
                        conn.getMetaData().getDatabaseProductVersion());
                    return;
                } catch (SQLException se) {
                    TestLogHolder.log("[JDBC-PREFLIGHT] FAIL " + t.label());
                    TestLogHolder.log("  SQLException: SQLState=%s, errorCode=%d, msg=%s", se.getSQLState(), se.getErrorCode(), se.getMessage());
                    if (se.getCause() != null) TestLogHolder.log("  cause=" + se.getCause());
                } catch (Exception e) {
                    TestLogHolder.log("[JDBC-PREFLIGHT] FAIL " + t.label());
                    TestLogHolder.log("  CUBRIDException: " + e.getMessage());
                    try {
                        var getErrorCode = e.getClass().getMethod("getErrorCode");
                        Object ec = getErrorCode.invoke(e);
                        TestLogHolder.log("  getErrorCode=" + ec);
                    } catch (Throwable ignore) {}
                    if (e.getCause() != null) {
                        TestLogHolder.log("  cause=" + e.getCause());
                    }
                } catch (Throwable th) {
                    TestLogHolder.log("[JDBC-PREFLIGHT] FAIL " + t.label());
                }
            }
            throw new AssertionError("[JDBC-PREFLIGHT] All attempts failed (A~D)");
        } catch (Throwable t) {
            throw new AssertionError("[JDBC] direct connect FAILED  (jar=" + driverJar + ")", t);

        }
    }

    private static String mask(String s) {
        return s == null ? "" : s.replaceAll(".", "*");
    }
}
