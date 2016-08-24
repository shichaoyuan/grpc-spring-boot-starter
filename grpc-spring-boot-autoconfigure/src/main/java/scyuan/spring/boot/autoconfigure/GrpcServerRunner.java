package scyuan.spring.boot.autoconfigure;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.serviceloader.ServiceFactoryBean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ReflectionUtils;
import scyuan.spring.boot.autoconfigure.annotation.GrpcService;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Created by yuanshichao on 16/2/18.
 */

public class GrpcServerRunner implements CommandLineRunner, DisposableBean {

    private static final Log LOG = LogFactory.getLog(GrpcServerRunner.class);

    private static final String BIND_SERVICE_METHOD_NAME = "bindService";

    private static final String GRPC_CLASS_NAME_POSTFIX = "Grpc";
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private GrpcServerProperties grpcServerProperties;

    private Server server;

    public void run(String... strings) throws Exception {
        LOG.info("Starting gRPC Server ...");

        final ServerBuilder<?> serverBuilder = ServerBuilder.forPort(grpcServerProperties.getPort());

        for (final Object grpcService : applicationContext.getBeansWithAnnotation(GrpcService.class).values()) {
            if (grpcService instanceof BindableService) {
                serverBuilder.addService((BindableService)grpcService);
                LOG.info("'" + grpcService.getClass().getSimpleName() + "' service has been registered.");
            } else {
                final Optional<Method> bindServiceMethod = Stream.of(grpcService.getClass().getInterfaces())
                        .map(aClass -> Optional.ofNullable(aClass.getEnclosingClass()))
                        .filter(Optional::isPresent)
                        .map(opt -> opt.get())
                        .filter(eClass -> eClass.getName().endsWith(GRPC_CLASS_NAME_POSTFIX))
                        .flatMap(eClass -> Stream.of(ReflectionUtils.getAllDeclaredMethods(eClass)))
                        .filter(method ->
                                BIND_SERVICE_METHOD_NAME.equals(method.getName())
                                        && method.getParameterCount() == 1
                                        && method.getParameterTypes()[0].isAssignableFrom(grpcService.getClass()))
                        .findFirst();
                if (bindServiceMethod.isPresent()) {
                    ServerServiceDefinition serviceDefinition
                            = (ServerServiceDefinition)bindServiceMethod.get().invoke(null, grpcService);
                    serverBuilder.addService(serviceDefinition);
                    LOG.info("'" + grpcService.getClass().getSimpleName() + "' service has been registered.");

                } else {
                    throw new IllegalArgumentException(
                            "Failed to find '" + BIND_SERVICE_METHOD_NAME + "' method for '" +
                                    grpcService.getClass().getName() + "' class.");
                }
            }
        }

        server = serverBuilder.build().start();
        LOG.info("gRPC Server started, listening on port " + grpcServerProperties.getPort());

        startDaemonAwaitThread();
    }

    public void destroy() throws Exception {
        LOG.info("Shutting down gRPC server ...");
        Optional.ofNullable(server).ifPresent(Server::shutdown);
        LOG.info("gRPC server stopped.");

    }

    private void startDaemonAwaitThread() {
        Thread awaitThread = new Thread() {
            @Override
            public void run() {
                try {
                    GrpcServerRunner.this.server.awaitTermination();
                } catch (InterruptedException e) {
                    LOG.error("gRPC server stopped.", e);
                }
            }
        };
        awaitThread.setDaemon(false);
        awaitThread.start();
    }
}
