package scyuan.spring.boot.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created by yuanshichao on 16/2/18.
 */

@ConfigurationProperties("grpc")
public class GrpcServerProperties {

    private int port = 6565;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
