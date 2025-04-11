import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages all the server of a single node.
 *
 * Generates all the servers of a single node, tracks the messages from the node's neighbor
 * and stores all of the received messages.
 */
public class ServersHandler extends Thread {
    public List<Integer> portsToSendFrom;
    private Integer nodeId;
    public Integer numberOfNodes;
    private Node node;
    private HashMap<Integer, Server> servers = new HashMap<>();
    public HashMap<Integer, Pair> acceptedPairs = new HashMap<>();
    public boolean canStop = false;
    public static Lock lock = new ReentrantLock();

    public ServersHandler(List<Integer> portsToSendFrom, int nodeId, int numberOfNodes, Node node) {
        this.portsToSendFrom = portsToSendFrom;
        this.nodeId = nodeId;
        this.numberOfNodes = numberOfNodes;
        this.node = node;
        for (int port : this.portsToSendFrom) {
            try {
                Server server = new Server(port, this, node.nodeId);
                servers.put(port, server);
                server.canStop = false;

            } catch (Exception e) {
                String a = e.toString();
            }
        }
    }

    /**
     * Tracking the received messages.
     *
     * This method runs until the related node has received messages from all of the nodes in the graph.
     * This method tracks the messages the servers received and stores the messages from all of the servers.
     */
    @Override
    public void run() {
        for (Map.Entry<Integer, Server> serverEntryentry : servers.entrySet()) {
            Server server = serverEntryentry.getValue();
            server.start();
        }

        this.node.lock.lock();
        HashMap<Integer, Pair> localPairs = (HashMap<Integer, Pair>) acceptedPairs.clone();
        this.node.lock.unlock();
        while (localPairs.size() != this.numberOfNodes - 1) {
            for (Map.Entry<Integer, Server> serverEntry : servers.entrySet()) {
                Server server = serverEntry.getValue();
                lock.lock();
                HashMap<Integer, Pair> nodePairs = (HashMap<Integer, Pair>) server.acceptedPairs.clone();
                lock.unlock();
                for (Map.Entry<Integer, Pair> entry : nodePairs.entrySet()) {
                    int key = entry.getKey();
                    if (key == this.nodeId) {
                        continue;
                    }
                    Pair val = entry.getValue();
                    this.node.lock.lock();
                    boolean isNewPair = localPairs.get(key) == null;
                    if (isNewPair) {
                        localPairs.put(key, val);
                    }
                    this.node.lock.unlock();
                }
            }
            this.node.lock.lock();
            acceptedPairs = localPairs;
            this.node.lock.unlock();
        }
        this.canStop = true;
    }

    public void close_servers(){
        for (Map.Entry<Integer, Server> entry : servers.entrySet()) {
            Server server = entry.getValue();
            server.close_connection();
            server.interrupt();
            server.canStop = true;
        }
    }
}
