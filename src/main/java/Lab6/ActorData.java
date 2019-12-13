package Lab6;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.japi.pf.ReceiveBuilder;

import java.util.ArrayList;
import java.util.Random;

public class ActorData extends AbstractActor {

    private ArrayList<Integer> portsList = new ArrayList<>();

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(
                        GetServersList.class, msg ->{
                            portsList = msg.getServersList();
                        }
                )
                .match(
                        GetRandomPort.class, port ->{

                            int randomIndex = new Random().nextInt(portsList.size());

                            while (portsList.get(randomIndex) == port.getPort()){
                                randomIndex = new Random().nextInt(portsList.size());
                            }

                            getSender().tell(portsList.get(randomIndex), ActorRef.noSender());
                        }
                )
                .build();
    }
}
