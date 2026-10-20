package dk.via.sep3.delivery.grpc;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Starter gRPC-serveren sammen med Spring og stopper den igen ved nedlukning. */
@Component
public class GrpcServerLifecycle implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(GrpcServerLifecycle.class);

    private final List<BindableService> services;
    private final int port;
    private Server server;

    public GrpcServerLifecycle(List<BindableService> services, @Value("${grpc.port:9090}") int port) {
        this.services = services;
        this.port = port;
    }

    @Override
    public void start() {
        ServerBuilder<?> builder = ServerBuilder.forPort(port);
        services.forEach(builder::addService);
        try {
            server = builder.build().start();
        } catch (IOException e) {
            throw new IllegalStateException("Kunne ikke starte gRPC på port " + port, e);
        }
        log.info("gRPC-server lytter på port {}", port);
    }

    @Override
    public void stop() {
        if (server != null) {
            server.shutdown();
            try {
                server.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public boolean isRunning() {
        return server != null && !server.isShutdown();
    }
}
