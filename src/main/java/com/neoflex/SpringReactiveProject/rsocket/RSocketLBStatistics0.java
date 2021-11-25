package com.neoflex.SpringReactiveProject.rsocket;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.neoflex.SpringReactiveProject.domain.Person;
import io.rsocket.RSocket;
import io.rsocket.client.LoadBalancedRSocketMono;
import io.rsocket.client.filter.RSocketSupplier;
import io.rsocket.core.RSocketConnector;
import io.rsocket.transport.netty.client.TcpClientTransport;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static io.rsocket.client.LoadBalancedRSocketMono.create;

public class RSocketLBStatistics0 {
    public static final String MIME_ROUTER = "message/x.rsocket.composite-metadata.v0";
    private static Logger log = (Logger) LoggerFactory.getLogger("ROOT");
    static {
        log.setLevel(Level.INFO); // turn off all DEBUG logging
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        RSocketStrategies strategies = RSocketStrategies.builder()
                .encoders(encoders -> encoders.add(new Jackson2JsonEncoder()))
                .decoders(decoders -> decoders.add(new Jackson2JsonDecoder()))
                .build();

        Function<Integer, Mono<RSocket>> getConnector = port ->
            Mono.from(RSocketConnector
                .create()
                .reconnect(Retry.fixedDelay(1000, Duration.ofSeconds(1)))
                .dataMimeType(MimeTypeUtils.APPLICATION_JSON_VALUE)
                .metadataMimeType(MIME_ROUTER)
                .connect(TcpClientTransport.create(port))
                .doOnSubscribe(s -> System.out.println("RSocket connection established on port " + port))
            );

        LoadBalancedRSocketMono balancer = create(
            Flux.just(7000, 7001)
            .map(port -> new RSocketSupplier(() -> getConnector.apply(port)))
            .collectList()
        );

        // we need to wait at least 1 RSocket in the balancer
        while (balancer.availability() == 0.0) {
            Thread.sleep(1);
        }

        AtomicInteger id = new AtomicInteger(1);
        Flux.range(1,5000)
                .doOnNext(i -> id.getAndIncrement())
                .flatMap(i -> balancer)
//                .retryWhen(Retry.fixedDelay(
//                        1000, Duration.ofSeconds(2))
//                        .doBeforeRetry(s -> System.out.println("retry 1 "+s+id.get())))
                .map(rSocket ->
                    RSocketRequester.wrap(rSocket,
                        MimeTypeUtils.APPLICATION_JSON,
                        MimeType.valueOf(MIME_ROUTER),
                        strategies))
                .flatMap(rSocket ->
                        rSocket
                        .route("findById2")
                        .data(id.get())
                        .retrieveMono(Person.class)
//                        .retryWhen(Retry.fixedDelay(
//                                10, Duration.ofSeconds(2))
//                                .doBeforeRetry(s -> System.out.println("retry 3 "+s+id.get())))
                )
                .retryWhen(Retry.fixedDelay(
                        1000, Duration.ofSeconds(2))
                        .doBeforeRetry(s -> System.out.println("retry 4 "+s+id.get())))
                //.groupBy(Person::getPort)
//                .flatMap(port ->
//                    Mono.zip(Mono.just(port.key()), port.count()))
//                .map(p -> p.getT1()+": "+p.getT2())
                .doOnNext(System.out::println)
                .blockLast();
        System.out.println(balancer);
    }
}
