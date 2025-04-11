import java.util.*;
import java.io.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Process the text input and manages the nodes.
 *
 * Reads the text input and generates the nodes accordingly.
 * In every update of any edge's weight, the ExManager notifies the relevant nodes.
 * Activates the nodes in every iteration.
 */
public class ExManager {
    private String path;
    private Integer num_of_nodes;
    private HashMap<Integer, Node> nodes = new HashMap<Integer, Node>();
    public static Lock lock = new ReentrantLock();

    public ExManager(String path){
        this.path = path;
    }

    public Node get_node(int id){
        return this.nodes.get(id);
    }

    public int getNum_of_nodes() {
        return this.num_of_nodes;
    }

    /**
     * Updates the edge's wight.
     *
     * This method notifies the relevant nodes about an update in the edge's weight.
     * @param  id1  first unique node's identifier
     * @param  id2  second unique node's identifier
     * @param  weight  the updated edge's weight
     */
    public void update_edge(int id1, int id2, double weight){
        Node node1 = this.nodes.get(id1);
        Node node2 = this.nodes.get(id2);
        node1.update_neighbour(id2, weight);
        node2.update_neighbour(id1, weight);
    }

    /**
     * Reads the input text and generates nodes accordingly.
     */
    public void read_txt() throws FileNotFoundException{
        Scanner scanner = new Scanner(new File(path));
        String line = scanner.nextLine();
        this.num_of_nodes = Integer.parseInt(line);
        lock.lock();
        while(scanner.hasNextLine()){
            line = scanner.nextLine();
            if(line.contains("stop")){
                return;
            }
            String[] strArray = line.split(" ");

            int newNodeId = Integer.parseInt(strArray[0]);
            Node newNode = new Node(newNodeId, num_of_nodes);

            for(int i=1; i<strArray.length; i+=4){
                //create node
                int neighbourId = Integer.parseInt(strArray[i]);
                double weight = Double.valueOf(strArray[i+1]);
                int sendingPort = Integer.parseInt(strArray[i+2]);
                int receivingPort = Integer.parseInt(strArray[i+3]);
                newNode.set_neighbour(neighbourId, weight, sendingPort, receivingPort);

            }
            this.nodes.put(newNodeId, newNode);
        }
        lock.unlock();
    }

    /**
     * Starts an iteration of "Link State Routing" algorithm.
     *
     * This method starts the node's threads.
     * It waits for all the threads to finish running and then
     * closes all the server connections.
     */
    public void start(){
        List<Thread> runningNodes= new ArrayList<Thread>();

        for (Map.Entry<Integer, Node> set : this.nodes.entrySet()) {
            Node node = set.getValue();
            Thread nodeThread = new Thread(node);
            runningNodes.add(nodeThread);
        }

        for (Thread nodeThread : runningNodes) {
            nodeThread.start();
        }
        for (Thread nodeThread : runningNodes) {
            try {
                nodeThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        for (Map.Entry<Integer, Node> set : this.nodes.entrySet()) {
            Node node = set.getValue();
            node.close_connections();
        }
    }

    public void terminate(){
        return;
    }
}
