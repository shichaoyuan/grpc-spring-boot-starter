package scyuan.spring.boot.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import scyuan.spring.boot.autoconfigure.annotation.EnableGrpcServer;

/**
 * Created by yuanshichao on 16/2/18.
 */

@SpringBootApplication
@EnableGrpcServer
public class SampleApp {

    public static void main(String[] args) {
        SpringApplication.run(SampleApp.class, args);
    }

}
