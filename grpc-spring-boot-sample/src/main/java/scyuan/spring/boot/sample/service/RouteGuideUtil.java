package scyuan.spring.boot.sample.service;

import com.google.protobuf.util.JsonFormat;
import scyuan.spring.boot.sample.routeguide.Feature;
import scyuan.spring.boot.sample.routeguide.FeatureDatabase;
import scyuan.spring.boot.sample.routeguide.Point;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.List;

/**
 * Created by yuanshichao on 2017/2/21.
 */
public class RouteGuideUtil {

    private static final double COORD_FACTOR = 1e7;

    /**
     * Gets the latitude for the given point.
     * @param location
     * @return
     */
    public static double getLatitude(Point location) {
        return location.getLatitude() / COORD_FACTOR;
    }

    /**
     * Gets the longitude for the given point.
     * @param location
     * @return
     */
    public static double getLongitude(Point location) {
        return location.getLongitude() / COORD_FACTOR;
    }

    /**
     * Gets the default features file from classpath.
     * @return
     */
    public static URL getDefaultFeaturesFile() {
        return RouteGuideUtil.class.getResource("/route_guide_db.json");
    }

    /**
     * Parses the JSON input file containing the list of features.
     * @param file
     * @return
     * @throws IOException
     */
    public static List<Feature> parseFeatures(URL file) throws IOException {
        InputStream input = file.openStream();

        try {
            Reader reader = new InputStreamReader(input);
            try {
                FeatureDatabase.Builder database = FeatureDatabase.newBuilder();
                JsonFormat.parser().merge(reader, database);
                return database.getFeatureList();
            } finally {
                reader.close();
            }
        } finally {
            input.close();
        }
    }

    /**
     * Indicates whether the given feature exists (i.e. has a valid name).
     * @param feature
     * @return
     */
    public static boolean exists(Feature feature) {
        return feature != null && !feature.getName().isEmpty();
    }

}
