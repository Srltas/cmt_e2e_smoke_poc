package com.cmt.e2e.support.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import com.cmt.e2e.support.Drivers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * db.conf와 같은 속성 기반 설정 파일을 관리하는 범용적인 추상 클래스
 */
public abstract class DbConfig {
    private static final Logger log = LoggerFactory.getLogger(DbConfig.class);

    protected final Path confPath;
    protected final String sourceName;
    protected final String targetName;

    protected DbConfig(Path confPath, String sourceName, String targetName) {
        this.confPath = confPath;
        this.sourceName = sourceName;
        this.targetName = targetName;
    }

    public Path getFinalConfPath() {
        return confPath;
    }

    public String getSourceHost() throws IOException {
        return readProp(this.sourceName + ".host");
    }

    public int getSourcePort() throws IOException {
        return Integer.parseInt(readProp(this.sourceName + ".port"));
    }

    public String getSourceDbName() throws IOException {
        return readProp(this.sourceName + ".dbname");
    }

    public String getSourceUser() throws IOException {
        return readProp(this.sourceName + ".user");
    }

    public String getSourcePassword() throws IOException {
        return readProp(this.sourceName + ".password");
    }

    public String getSourceCharset() throws IOException {
        return readProp(this.sourceName + ".charset");
    }

    public Path getSourceDriverJarPath() {
        return Drivers.latest(Drivers.DB.CUBRID);
    }

    protected void upsertProperty(String key, String value) throws IOException {
        log.debug("Patching {} with: {}={}", confPath, key, value);
        List<String> lines = Files.readAllLines(confPath, StandardCharsets.UTF_8);

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
        Files.write(confPath, lines, StandardCharsets.UTF_8);
    }

    protected String readProp(String key) throws IOException {
        for (String line : Files.readAllLines(confPath)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int eq = line.indexOf('=');
            if (eq <= 0) continue;
            String k = line.substring(0, eq).trim();
            if (k.equals(key)) {
                return line.substring(eq + 1).trim();
            }
        }
        return "";
    }
}
