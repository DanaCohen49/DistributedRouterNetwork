import java.net.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Responsible for sending messages.
 *
 * Every instance of this class is related to a single edge in the graph.
 * This class represents one of the nodes in the edge, and from here this
 * node will send messages to the other node.
 */
public class Client extends Thread {
    public Integer port;
    public Socket socket;
    public ObjectOutputStream out;
    public static boolean canCloseConnection = false;

    public Client(int port) {
        this.port = port;
    }

    /**
     * Sends the first package.
     *
     * This method makes the first connection attempt and sends the node's lv.
     */
    @Override
    public void run() {
        boolean triedReconnect = false;
        while (socket == null && !canCloseConnection) {
            try {
                socket = new Socket("localhost", port);
                this.out = new ObjectOutputStream(socket.getOutputStream());
            } catch (Exception e) {
                String a = e.toString();
                if (triedReconnect){
                    return;
                }
                try {
                    socket.close();
                } catch (Exception ex) {
                    String s = ex.toString();
                    triedReconnect = true;
                }
            }
        }

    }

    /**
     * Sends packages.
     *
     * This method creates a thread that will send the given packages to one of the node's neighbors.
     * The packages that we will send here are all packages that the related node has received from
     * other nodes, and in this method the node sends those packages to one of her neighbors.
     */
    public void send_package(HashMap<Integer, Pair> packagesToSend) {
        Thread sendPackage = new Thread(new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<Integer, Pair> entry : packagesToSend.entrySet()) {
                    Pair packageToSend = entry.getValue();
                    boolean sent = false;
                    while (!sent) {
                        try {
                            if (out == null) {
                                // we will get here only on the first iteration of the for loop.
                                socket = new Socket("localhost", port);
                                out = new ObjectOutputStream(socket.getOutputStream());
                            }
                            out.writeObject(packageToSend);
                            sent = true;
                        } catch (IOException e) {
                            //here we handle different failure scenarios until we will successfully acquire connection
                            //or until we will be informed we can terminate(happens if all the nodes received all
                            //the messages already).
                            String s = e.toString();
                            try {
                                if (socket != null) {
                                    socket.close();
                                }
                                if (out != null) {
                                    out.close();
                                }
                                socket = new Socket("localhost", port);
                                out = new ObjectOutputStream(socket.getOutputStream());
                            } catch (IOException ex) {
                                if (canCloseConnection) {
                                    return;
                                }
                            }
                        }
                    }
                }
                try {
                    if (out !=null){
                        out.close();
                    }
                    if (socket != null){
                        socket.close();
                    }
                } catch (IOException e) {
                    String k = e.toString();
                }
                out = null;
                socket = null;
            }
        });

        sendPackage.start();
    }
}
