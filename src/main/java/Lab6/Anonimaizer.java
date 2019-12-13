package Lab6;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.pattern.Patterns;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import org.omg.CORBA.TIMEOUT;

import java.io.IOException;
import java.time.Duration;
import java.util.Scanner;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static akka.actor.TypedActor.context;

public class Anonimaizer extends AllDirectives {

    private static Http http;
    private static int serverPort;

    private static ActorRef actorData;

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        serverPort = scanner.nextInt();

        ActorSystem system = ActorSystem.create("routes");
        actorData = system.actorOf(Props.create(ActorData.class));

        http = Http.get(context().system());

        final ActorMaterializer materializer = ActorMaterializer.create(system);

        Anonimaizer anonimaizer = new Anonimaizer();

        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = anonimaizer.routes().flow(system, materializer);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(
                routeFlow,
                ConnectHttp.toHost("127.0.0.1", serverPort),
                materializer
        );

        System.out.println("Server online on port "+ serverPort);

        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        binding
                .thenCompose(ServerBinding::unbind)
                .thenAccept(unbound -> system.terminate());



    }

    CompletionStage<HttpResponse> fetchToServer(String url, int port, int count) {
        return http.singleRequest(HttpRequest.create(url));
    }

    CompletionStage<HttpResponse> fetch(String url) {
        return http.singleRequest(HttpRequest.create(url));
    }

    private Route routes() {
        return get(
                parameter("url", url->
                        parameter("count", count->{
                            int countInt = Integer.parseInt(count);
                            if (countInt==0){
                                try {
                                    return complete(fetch(url).toCompletableFuture().get());
                                } catch (InterruptedException | ExecutionException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                CompletionStage<HttpResponse> newPort = Patterns.ask(
                                        actorData,
                                        serverPort,
                                        Duration.ofMillis(5000)
                                ).thenCompose(
                                        port ->
                                                fetchToServer(
                                                        url,
                                                        (Integer) port,
                                                        countInt - 1
                                                )
                                );
                                return completeWithFuture(newPort);
                            }





                        })


                        )





        )
    }




}



