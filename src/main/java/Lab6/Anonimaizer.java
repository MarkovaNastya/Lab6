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
import org.apache.zookeeper.*;


import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;


import static akka.actor.TypedActor.context;

public class Anonimaizer extends AllDirectives {

    private final static String ERROR_404 = "ERROR 404";

    private static Http http;
    private static int serverPort;
    private static ZooKeeper zoo;

    private static ActorRef actorData;

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        serverPort = scanner.nextInt();

        ActorSystem system = ActorSystem.create("routes");
        actorData = system.actorOf(Props.create(ActorData.class));

        try {
            zoo();
        } catch (IOException | KeeperException | InterruptedException e) {
            e.printStackTrace();
        }

        http = Http.get(context().system());

        final ActorMaterializer materializer = ActorMaterializer.create(system);

        Anonimaizer anonimaizer = new Anonimaizer();

        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = anonimaizer.route().flow(system, materializer);
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

    public static class ConnectWatcher implements Watcher {

        @Override
        public void process(WatchedEvent watchedEvent) {
            List<String> portsInfo = new ArrayList<>();

            try {
                portsInfo = zoo.getChildren(
                        "/servers",
                        this
                );
            } catch (KeeperException | InterruptedException e) {
                e.printStackTrace();
            }

            List<String> ports = new ArrayList<>();

            for (String portInfo : portsInfo) {
                byte[] data = new byte[0];
                try {
                    data = zoo.getData("/servers/" + portInfo, false, null);
                } catch (KeeperException | InterruptedException e) {
                    e.printStackTrace();
                }
                String portData = new String(data);
                ports.add(portData);
            }

            ArrayList<Integer> portsInt = new ArrayList<>();
            for (String port : ports) {
                portsInt.add(Integer.parseInt(port));
            }

            actorData.tell(new GetServersList(portsInt), ActorRef.noSender());

        }
    }

    private static void zoo() throws IOException, KeeperException, InterruptedException {
        zoo = new ZooKeeper(
                "127.0.0.1:2181",
                5000,
                new ConnectWatcher()
        );

        zoo.create(
                "/servers",
                "parent".getBytes(),
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT
        );

        zoo.create(
                "/servers/" + serverPort,
                Integer.toString(serverPort).getBytes(),
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL
        );

        zoo.getChildren(
                "/servers",
                new ConnectWatcher()
        );


    }

    CompletionStage<HttpResponse> fetchToServer(String url, int port, int count) {
        String req = "http://localhost:" + port + "/?url=" + url + "&count=" + count;
        return http.singleRequest(HttpRequest.create(req));
    }

    CompletionStage<HttpResponse> fetch(String url) {
        return http.singleRequest(HttpRequest.create(url));
    }

    private Route route() {
        return get(
                () -> parameter("url", url->
                        parameter("count", count -> {
                                int countInt = Integer.parseInt(count);
                                if (countInt == 0) {
                                    try {
                                        return complete(fetch(url).toCompletableFuture().get());
                                    } catch (InterruptedException | ExecutionException e) {
                                        e.printStackTrace();
                                        return complete(ERROR_404);
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
                                                            (int) port,
                                                            countInt - 1
                                                    )
                                    );
                                    return completeWithFuture(newPort);
                                }
                        })
                )
        );
    }




}



