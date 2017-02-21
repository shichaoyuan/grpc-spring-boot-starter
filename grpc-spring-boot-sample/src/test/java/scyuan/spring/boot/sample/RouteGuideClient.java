package scyuan.spring.boot.sample;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scyuan.spring.boot.sample.routeguide.*;
import scyuan.spring.boot.sample.service.RouteGuideUtil;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by yuanshichao on 2017/2/21.
 */
public class RouteGuideClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(RouteGuideClient.class);

    private final ManagedChannel channel;
    private final RouteGuideGrpc.RouteGuideBlockingStub blockingStub;
    private final RouteGuideGrpc.RouteGuideStub asyncStub;

    private Random random = new Random();

    public RouteGuideClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext(true));
    }

    public RouteGuideClient(ManagedChannelBuilder<?> channelBuilder) {
        this.channel = channelBuilder.build();
        this.blockingStub = RouteGuideGrpc.newBlockingStub(channel);
        this.asyncStub = RouteGuideGrpc.newStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void getFeature(int lat, int lon) {
        LOGGER.info("*** GetFeature: lat={} lon={}", lat, lon);

        Point request = Point.newBuilder().setLatitude(lat).setLongitude(lon).build();

        Feature feature;

        try {
            feature = blockingStub.getFeature(request);
        } catch (StatusRuntimeException e) {
            LOGGER.warn("RPC failed: {}", e.getStatus());
            return;
        }

        if (RouteGuideUtil.exists(feature)) {
            LOGGER.info("Found feature called \"{}\" at {}, {}",
                    feature.getName(),
                    RouteGuideUtil.getLatitude(feature.getLocation()),
                    RouteGuideUtil.getLongitude(feature.getLocation()));
        } else {
            LOGGER.info("Found no feature at {}, {}",
                    RouteGuideUtil.getLatitude(feature.getLocation()),
                    RouteGuideUtil.getLongitude(feature.getLocation()));
        }

    }

    /**
     * Blocking server-streaming example. Calls listFeatures with a rectangle of interest. Prints each
     * response feature as it arrives.
     */
    public void listFeatures(int lowLat, int lowLon, int hiLat, int hiLon) {
        LOGGER.info("*** ListFeatures: lowLat={} lowLon={} hiLat={} hiLon={}", lowLat, lowLon, hiLat, hiLon);

        Rectangle request = Rectangle.newBuilder()
                .setLo(Point.newBuilder().setLatitude(lowLat).setLongitude(lowLon).build())
                .setHi(Point.newBuilder().setLatitude(hiLat).setLongitude(hiLon).build())
                .build();

        Iterator<Feature> featureIterator;

        try {
            featureIterator = blockingStub.listFeatures(request);
            for (int i = 1; featureIterator.hasNext(); i++) {
                Feature feature = featureIterator.next();
                LOGGER.info("Result #{}: {}", i, feature);
            }
        } catch (StatusRuntimeException e) {
            LOGGER.warn("RPC failed: {}", e.getStatus());
        }
    }

    /**
     * Async client-streaming example. Sends {@code numPoints} randomly chosen points from {@code
     * features} with a variable delay in between. Prints the statistics when they are sent from the
     * server.
     */
    public void recordRoute(List<Feature> features, int numPoints) throws InterruptedException {
        LOGGER.info("*** RecordRoute");

        final CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<RouteSummary> responseObserver = new StreamObserver<RouteSummary>() {
            @Override
            public void onNext(RouteSummary value) {
                LOGGER.info("Finished trip with {} points. Passed {} features. Travelled {} meters. It took {} seconds.",
                        value.getPointCount(), value.getFeatureCount(), value.getDistance(), value.getElapsedTime());
            }

            @Override
            public void onError(Throwable t) {
                LOGGER.warn("RecordRoute Failed: {}", Status.fromThrowable(t));
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                LOGGER.info("Finished RecordRoute");
                finishLatch.countDown();
            }
        };

        StreamObserver<Point> requestObserver = asyncStub.recordRoute(responseObserver);
        try {
            for (int i = 0; i < numPoints; i++) {
                int index = random.nextInt(features.size());
                Point point = features.get(index).getLocation();
                LOGGER.info("Visiting point {}, {}",
                        RouteGuideUtil.getLatitude(point),
                        RouteGuideUtil.getLongitude(point));
                requestObserver.onNext(point);
                // Sleep for a bit before sending the next one.
                Thread.sleep(random.nextInt(1000) + 500);
                if (finishLatch.getCount() == 0) {
                    // RPC completed or errored before we finished sending.
                    // Sending further requests won't error, but they will just be thrown away.
                    return;
                }
            }
        } catch (RuntimeException e) {
            // Cancel RPC
            requestObserver.onError(e);
            throw e;
        }

        requestObserver.onCompleted();

        if (!finishLatch.await(1, TimeUnit.MINUTES)) {
            LOGGER.warn("recordRoute can not finish within 1 minutes");
        }
    }

    /**
     * Bi-directional example, which can only be asynchronous. Send some chat messages, and print any
     * chat messages that are sent from the server.
     */
    public CountDownLatch routeChat() {
        LOGGER.info("*** RouteChat");

        final CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<RouteNote> requestObserver = asyncStub.routeChat(new StreamObserver<RouteNote>() {
            @Override
            public void onNext(RouteNote value) {
                LOGGER.info("Got message \"{}\" at {}, {}",
                        value.getMessage(),
                        value.getLocation().getLatitude(),
                        value.getLocation().getLongitude());
            }

            @Override
            public void onError(Throwable t) {
                LOGGER.warn("RouteChat Failed: {}", Status.fromThrowable(t));
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                LOGGER.info("Finished RouteChat");
                finishLatch.countDown();

            }
        });

        try {
            RouteNote[] requests = {
                    newNote("First message", 0, 0),
                    newNote("Second message", 0, 1),
                    newNote("Third message", 1, 0),
                    newNote("Fourth message", 1, 1)
            };

            for (RouteNote request : requests) {
                LOGGER.info("Sending message \"{}\" at {}, {}",
                        request.getMessage(),
                        request.getLocation().getLatitude(),
                        request.getLocation().getLongitude());
                requestObserver.onNext(request);
            }
        } catch (RuntimeException e) {
            requestObserver.onError(e);
            throw e;
        }

        requestObserver.onCompleted();

        return finishLatch;
    }

    private RouteNote newNote(String message, int lat, int lon) {
        return RouteNote.newBuilder().setMessage(message)
                .setLocation(Point.newBuilder().setLatitude(lat).setLongitude(lon).build()).build();
    }


    public static void main(String[] args) throws InterruptedException {

        ch.qos.logback.classic.Logger root =
                (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(
                        ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        root.setLevel(ch.qos.logback.classic.Level.INFO);

        List<Feature> features;
        try {
            features = RouteGuideUtil.parseFeatures(RouteGuideUtil.getDefaultFeaturesFile());
        } catch (IOException e) {
            LOGGER.error("", e);
            return;
        }

        RouteGuideClient client = new RouteGuideClient("localhost", 6565);

        try {
            // Looking for a valid feature
            client.getFeature(409146138, -746188906);

            // Feature missing.
            client.getFeature(0, 0);

            // Looking for features between 40, -75 and 42, -73.
            client.listFeatures(400000000, -750000000, 420000000, -730000000);

            // Record a few randomly selected points from the features file.
            client.recordRoute(features, 10);

            // Send and receive some notes
            CountDownLatch finishLatch = client.routeChat();

            if (!finishLatch.await(1, TimeUnit.MINUTES)) {
                LOGGER.warn("routeChat can not finish within 1 minutes");
            }

        } finally {
            client.shutdown();
        }
    }

}
