import java.net.*;
import java.util.*;
import java.io.*;

/**
 * Responsible for receiving messages.
 *
 * Every instance of this class is related to a single edge in the graph.
 * This class represents one of the nodes in the edge, and from here this
 * node will receive messages from the other node.
 */
public class Server extends Thread {
    public Socket client = null;
    public ServerSocket server;
    public HashMap<Integer, Pair> acceptedPairs = new HashMap<>();
    private ServersHandler handler;
    public static boolean canStop = false;
    private ObjectInputStream in = null;
    private Integer nodeId;

    public Server(int port, ServersHandler handler, int nodeId) {
        this.handler = handler;
        this.nodeId = nodeId;
        while (server == null) {
            try {
                server = new ServerSocket();
                server.setReuseAddress(true);
                server.bind(new InetSocketAddress("localhost", port));
            } catch (IOException e) {
                String error = e.toString();
            }
        }
    }

    /**
     * Receives messages from neighbor.
     *
     * In this method, first we will wait for a new client connection.
     * Once we will be connected to a client, we will receive messages and store them until the client
     * stops sending messages and disconnects.
     * After that, we will keep repeating the process of waiting for a new client connection,
     * receiving and storing messages.
     * This method stops once we receive messages from all of the nodes in the graph,
     * or when we will be notified that our handler already received messages from all of the nodes.
     */
    @Override
    public void run() {
        while (client == null) {
            try {
                client = server.accept();
                client.setReuseAddress(true);
                in = new ObjectInputStream(client.getInputStream());
            } catch (IOException e) {
                String error = e.toString();
            }
        }
        while (acceptedPairs.size() < this.handler.numberOfNodes -1) {
            while (in == null) {
                try {
                    in = new ObjectInputStream(client.getInputStream());
                }
                catch (Exception s){
                    try {
                        //if we got here, we need to terminate
                        server.close();
                        if (client != null){
                            client.close();
                        }
                        if (in != null){
                            in.close();
                        }
                        if (this.handler.canStop){
                            return;
                        }
                        client = server.accept();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                Object inputObject = in.readObject();
                Pair inputPair = (Pair) inputObject;
                int sendingId = (int) inputPair.getKey();
                if (sendingId == nodeId){
                    continue;
                }
                if (acceptedPairs.containsKey(sendingId)){
                    continue;
                }
                handler.lock.lock();
                acceptedPairs.put(sendingId, inputPair);
                handler.lock.unlock();
            } catch (Exception e) {
                //most probably we just need to return because we are done (the handler already received
                //all the messages).
                if (canStop){
                    return;
                }
                //if we got here, it means we will continue receiving packages and we should wait for a new client's connection
                String s = e.toString();
                try {
                    if (client != null){
                        client.close();
                    }
                    client = server.accept();
                    in = null;
                } catch (IOException ex) {
                    String ss = ex.toString();
                }
            }
        }
    }
    public void close_connection(){
        try {
            client.close();
            if (in != null){
                in.close();
            }

            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
