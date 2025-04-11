import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents and manages a single node in the graph.
 *
 * This class executes the run of "Link State Routing" algorithm from the perspective of a single node.
 * In this class we hold and maintain the information that the node has about the graph.
 * In every iteration of the algorithm, the process of receiving and sending messages
 * is being activated in this class.
 */
public class Node extends Thread {
    public HashMap<Integer, Object[]> neighbours = new HashMap<>();
    public HashMap<Integer, Integer> portsToListenToByNeighbors = new HashMap<>();
    public Integer nodeId;
    public Integer num_of_nodes;
    public double[][] adjacencyMatrix;
    public List<Integer> portsToListenTo = new ArrayList<>();
    public List<Integer> portsToSendFrom = new ArrayList<>();
    public static Lock lock = new ReentrantLock();
    private ClientsHandler clientsHandler;
    private ServersHandler serversHandler;

    public Node(int nodeId, int num_of_nodes) {
        this.nodeId = nodeId;
        this.num_of_nodes = num_of_nodes;
        this.adjacencyMatrix = new double[num_of_nodes][num_of_nodes];
        for (int i = 0; i < num_of_nodes; i++) {
            for (int j = 0; j < num_of_nodes; j++) {
                this.adjacencyMatrix[i][j] = -1;
            }
        }
    }

    /**
     * Sets new neighbor.
     *
     * This method adds new neighbor to the neighbours HashMap.
     * @param  neighbourId  the neighbor's unique identifier
     * @param  weight  edge's weight
     * @param  sendingPort  the port for sending messages to the neighbor
     * @param  receivingPort  the port for receiving messages from the neighbor
     */
    public void set_neighbour(int neighbourId, double weight, int sendingPort, int receivingPort) {
        this.neighbours.put(neighbourId, new Object[]{weight, sendingPort, receivingPort});
        this.adjacencyMatrix[this.nodeId - 1][neighbourId - 1] = weight;
        this.portsToListenTo.add(receivingPort);
        this.portsToSendFrom.add(sendingPort);
        this.portsToListenToByNeighbors.put(neighbourId, receivingPort);
    }

    /**
     * Runs an iteration of "Link State Routing" algorithm from this node's perspective.
     *
     * This method generates an instance ServersHandler and an instance of ClientsHandler.
     * Using the ClientsHandler, it sends to all the neighbors the current information the node has on the graph - lv.
     * Then, the node uses ServersHandler to track new message it received and send those to her neighbors using
     * ClientsHandler. It will continue doing that until it received messages from all of the nodes and sent all
     * the messages to all of her neighbors.
     * Then, using the ClientsHandler the method will send all the messages she received from all the nodes for the
     * second time.
     * After that, the node will update the adjacency matrix and finish.
     */
    @Override
    public void run(){
        Double[] lv = create_lv();
        Pair packageToSend = new Pair(nodeId, lv);

        serversHandler = new ServersHandler(this.portsToSendFrom, this.nodeId, this.num_of_nodes, this);
        clientsHandler = new ClientsHandler(this.portsToListenTo, this, packageToSend, portsToListenToByNeighbors);

        serversHandler.start();
        clientsHandler.start();

        Set<Integer> sentPairs = new HashSet<>();
        while (sentPairs.size() != this.num_of_nodes - 1) {
            lock.lock();
            HashMap<Integer, Pair> serverPairs = (HashMap<Integer, Pair>) serversHandler.acceptedPairs.clone();
            lock.unlock();
            serverPairs.keySet().remove(sentPairs);
            //after the next line, serverPairs will contain only the pair we didn't send yet.
            serverPairs.entrySet().removeIf(ent -> sentPairs.contains(ent.getKey()));
            if (serverPairs.size() == 0){
                continue;
            }
            clientsHandler.send_packages(serverPairs);
            for (Map.Entry<Integer, Pair> entry : serverPairs.entrySet()) {
                int key = entry.getKey();
                sentPairs.add(key);
            }
        }

    lock.lock();
    HashMap<Integer, Pair> serverPairs = (HashMap<Integer, Pair>) serversHandler.acceptedPairs.clone();
    lock.unlock();

    HashMap<Integer, Pair> resendPackages = new HashMap<>();
    for (Map.Entry<Integer, Pair> entry : serverPairs.entrySet()) {
        int key = entry.getKey();
        if (neighbours.containsKey(key)) {
            continue;
        }
        Pair val = entry.getValue();
        resendPackages.put(key, val);
    }
    clientsHandler.send_packages(resendPackages);
    update_matrix(serverPairs);
    }

    /**
     * Updates edge's weight.
     *
     * This method updates the weight of an edge that is connected to this node.
     * @param  neighbourId  the neighbor's unique identifier
     * @param  newWeight  new edge's weight
     */
    public void update_neighbour(int neighbourId, double newWeight) {
        Object[] value = this.neighbours.get(neighbourId);
        value[0] = newWeight;
        this.neighbours.put(neighbourId, value);
        this.adjacencyMatrix[this.nodeId - 1][neighbourId - 1] = newWeight;
    }

    public void print_graph() {
        for (int row = 0; row < num_of_nodes; row++) {
            for (int col = 0; col < num_of_nodes; col++) {
                if (col == num_of_nodes -1){
                    System.out.print(this.adjacencyMatrix[row][col]);
                }
                else {
                    System.out.print(this.adjacencyMatrix[row][col] + ", ");
                }
            }
            System.out.println();
        }
    }

    public void close_connections(){
        clientsHandler.close();
        serversHandler.close_servers();
        clientsHandler.interrupt();
        serversHandler.interrupt();
    }

    private void update_matrix(HashMap<Integer, Pair> acceptedPairs){
        for(int i = 0; i<this.num_of_nodes; i++){
            if (i +1 == this.nodeId ){
                continue;
            }
            Pair pair = acceptedPairs.get(i+1);
            int sendingNeighbor = ((int) pair.getKey());
            Double[] sendingLv = (Double[])pair.getValue();
            for (int col = 0; col < num_of_nodes; col++) {
                if(sendingLv[col] != null){
                    this.adjacencyMatrix[sendingNeighbor-1][col] = sendingLv[col];
                }
            }
        }
    }

    private Double[] create_lv(){
        Double[] lv = new Double[this.num_of_nodes];
        for (Map.Entry<Integer,Object[]> entry : this.neighbours.entrySet()) {
            int key = (int) entry.getKey();
            Object[] value = (Object[]) entry.getValue();
            lv[key-1] = (Double) value[0];
        }
        return lv;
    }
}
