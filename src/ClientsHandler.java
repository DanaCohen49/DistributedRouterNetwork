import java.io.*;
import java.net.Socket;
import java.util.*;

/**
 * Manages all the clients of a node.
 *
 * Generates all the clients of a node and sends messages to the related node's neighbors.
 */
public class ClientsHandler extends Thread {
    public List<Integer> portsToListenTo;
    public List<Client> clients = new ArrayList<>();
    private Node node;
    private Pair packageToSend;
    private HashMap<Socket, ObjectOutputStream> sockets = new HashMap<>();
    public HashMap<Integer, Integer> portsToListenToByNeighbors;

    public ClientsHandler(List<Integer> portsToListenTo, Node node, Pair packageToSend, HashMap<Integer, Integer> portsToListenToByNeighbors){
            this.portsToListenTo = portsToListenTo;
            this.node = node;
            this.packageToSend = packageToSend;
            this.portsToListenToByNeighbors = portsToListenToByNeighbors;
    }

    /**
     * Sends the first message to all the node's neighbors.
     *
     * This method creates new client instances, each instance is related to one port.
     * Then, this method sends the first message which contains the node's lv to all of the node's neighbors.
     */
    @Override
    public void run() {
        for (int port : this.portsToListenTo) {
            try {
                Client client = new Client(port);
                client.start();
                clients.add(client);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for(Client client:clients){
            try {
                client.join();
                HashMap<Integer, Pair> firstPackage = new HashMap<>();
                firstPackage.put(this.node.nodeId, this.packageToSend);
                client.send_package(firstPackage);
                client.canCloseConnection = false;
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * Sends messages to all the node's neighbors.
     *
     * This method creates new client instances, each instance is related to one port.
     * Then, this method sends the messages to all of the node's neighbors.
     * Each message is a message that the node received from another node in the graph, and each message
     * contains the other node's lv.
     */
    public void send_packages(HashMap<Integer, Pair> newPackages) {
        List<Client> clients = new ArrayList<>();
        for (int port : this.portsToListenTo) {
            try {
                Client client = new Client(port);
                client.start();
                clients.add(client);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (Client client : clients) {
            try {
                client.join();
                client.send_package(newPackages);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void close(){
        for (Map.Entry<Socket, ObjectOutputStream> entry : sockets.entrySet()) {
            Socket socket = entry.getKey();
            ObjectOutputStream out = entry.getValue();
            try {
                out.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (Client client: clients){
            client.interrupt();
            client.canCloseConnection = true;
        }
    }
}
