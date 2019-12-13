package Lab6;

import akka.actor.AbstractActor;
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
                        Integer.class, msg ->{

                            int randomIndex = new Random().nextInt(portsList.size());

                            while (portsList.get(randomIndex)==msg)


                        }


                )
                .build();
    }
}
