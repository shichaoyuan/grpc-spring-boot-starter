package scyuan.spring.boot.autoconfigure.annotation;

import org.springframework.context.annotation.Import;
import scyuan.spring.boot.autoconfigure.GrpcServerAutoConfiguration;

import java.lang.annotation.*;

/**
 * Created by yuanshichao on 16/3/22.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Import(GrpcServerAutoConfiguration.class)
public @interface EnableGrpcServer {
}
