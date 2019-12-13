package Lab6;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.CompletionStage;

import static akka.actor.TypedActor.context;

public class Anonimaizer extends AllDirectives {

    private static Http http;

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        int serverPort = scanner.nextInt();

        ActorSystem system = ActorSystem.create("routes");

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


    CompletionStage<HttpResponse> fetch(String url) {
        return http.singleRequest(HttpRequest.create(url));
    }

    private Route routes() {
        return null;
    }




}



