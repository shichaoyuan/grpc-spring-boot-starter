package scyuan.spring.boot.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import scyuan.spring.boot.autoconfigure.annotation.GrpcService;

/**
 * Created by yuanshichao on 16/2/18.
 */

@Configuration
@ConditionalOnClass(GrpcService.class)
@EnableConfigurationProperties(GrpcServerProperties.class)
public class GrpcServerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(GrpcServerRunner.class)
    public GrpcServerRunner grpcServerRunner() {
        return new GrpcServerRunner();
    }

}
