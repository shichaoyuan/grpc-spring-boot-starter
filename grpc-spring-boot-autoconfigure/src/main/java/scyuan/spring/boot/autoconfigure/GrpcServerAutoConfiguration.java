package scyuan.spring.boot.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by yuanshichao on 16/2/18.
 */

@Configuration
@EnableConfigurationProperties(GrpcServerProperties.class)
public class GrpcServerAutoConfiguration {

    @Bean
    @ConditionalOnBean(annotation = GrpcService.class)
    public GrpcServerRunner grpcServerRunner() {
        return new GrpcServerRunner();
    }

}
