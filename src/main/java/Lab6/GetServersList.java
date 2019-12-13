package Lab6;

import java.util.ArrayList;

public class GetServersList {

    private ArrayList<Integer> serversList = new ArrayList<>();

    public GetServersList(ArrayList<Integer> serversList) {
        this.serversList = serversList;
    }

    public ArrayList<Integer> getServersList() {
        return serversList;
    }
}
