package scyuan.spring.boot.sample.service;

import io.grpc.stub.StreamObserver;
import scyuan.spring.boot.autoconfigure.annotation.GrpcService;
import scyuan.spring.boot.sample.helloworld.GreeterGrpc;
import scyuan.spring.boot.sample.helloworld.HelloReply;
import scyuan.spring.boot.sample.helloworld.HelloRequest;

/**
 * Created by yuanshichao on 16/2/18.
 */

@GrpcService
public class GreeterService implements GreeterGrpc.Greeter {
    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + request.getName()).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
}
