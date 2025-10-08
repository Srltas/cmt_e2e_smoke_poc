package com.cmt.e2e.support.containers;

import java.time.Duration;
import java.util.Set;

import com.cmt.e2e.support.Drivers.DB;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.utility.DockerImageName;

public class CubridDemodbContainer implements DatabaseContainer {
    private static final DockerImageName IMAGE = DockerImageName.parse("cubriddmkim/cubrid_demodb:11.4");
    private static final int CUBRID_PORT = 33000;

    private final GenericContainer<?> container;

    public CubridDemodbContainer() {
        this.container = new GenericContainer<>(IMAGE)
            .withPrivilegedMode(true)
            .withEnv("CUBRID_COMPONENTS", "DEMO")
            .withExposedPorts(CUBRID_PORT)
            .waitingFor(Wait.forListeningPort())
            .withStartupTimeout(Duration.ofMinutes(2));
    }

    @Override
    public String getHost() {
        return container.getHost();
    }

    @Override
    public Integer getDatabasePort() {
        return container.getMappedPort(CUBRID_PORT);
    }

    @Override
    public DB getDbType() {
        return DB.CUBRID;
    }

    @Override
    public GenericContainer<?> getContainer() {
        return container;
    }

    @Override
    public void start() {
        container.start();
    }

    @Override
    public void stop() {
        container.stop();
    }

    @Override
    public Set<Startable> getDependencies() {
        return container.getDependencies();
    }
}
