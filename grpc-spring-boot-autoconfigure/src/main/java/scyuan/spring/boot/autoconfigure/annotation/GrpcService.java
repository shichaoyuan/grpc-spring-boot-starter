package scyuan.spring.boot.autoconfigure.annotation;

import org.springframework.stereotype.Service;

import java.lang.annotation.*;

/**
 * Created by yuanshichao on 16/2/18.
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Service
public @interface GrpcService {
}
