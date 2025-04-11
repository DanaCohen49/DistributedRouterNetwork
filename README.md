# 🌐 Distributed Router Network

This Java project simulates a **distributed router network** that runs the **Link-State Routing Algorithm**.  
It was developed as part of a distributed systems course and models how routers in a network can build and maintain a full topology using **only local communication**.

---

## 📚 Overview

Each **router node** in the network:

✅ Reads neighbors, edge weights & port info from a structured input file.  
✅ Communicates **only with direct neighbors** using TCP sockets.  
✅ Participates in a **distributed link-state routing algorithm**.  
✅ Builds its own view of the full topology matrix (adjacency matrix).  
✅ Handles **edge weight updates** and recomputes paths as needed.  
✅ Can print its current view of the graph.

The simulation is orchestrated using the `ExManager` class.

---

## 📁 Project Structure

```
src/
├── Node.java           # Represents a router node (runs as a thread)
├── ExManager.java      # Graph manager: builds nodes, updates edges, starts routing
├── Pair.java           # Serializable key-value pair class
├── main.java           # Program entry point
out/                    # Compiled class files
.idea/                  # IntelliJ config (optional)
DistDataSys.iml         # IntelliJ project module file
```

---

## 🔌 Communication Design

🧠 Each node opens a TCP socket per neighbor — two ports per connection (send/receive).  
🧵 Each node spawns threads for listening and routing in parallel.  
📦 Data is transferred using serialized Java objects (`ObjectInputStream` / `ObjectOutputStream`).  
🚫 Nodes never modify each other's data directly.

---

## 📜 Input File Format

The input file defines the topology like so:

```
<number of nodes>
<node_id> <neighbor_id> <weight> <send_port> <listen_port> ...
...
stop
<future instructions – ignored by ExManager>
```

🔍 **Example:**
```
4
1 2 5.0 10001 10002 3 3.5 10003 10004
2 1 5.0 10002 10001
3 1 3.5 10004 10003
4 ...
stop
```

🧩 Notes:
- Each line describes a single node and its neighbors.
- Port mappings must be symmetric between nodes.
- `stop` marks the end of the topology definition.

---

## 🚀 How to Run

1. **Compile** the project:

```bash
javac -d out src/*.java
```

2. **Run** the simulation:

```bash
java -cp out main input1.txt
```

3. Watch as nodes print their **adjacency matrices** after routing convergence.

> 💡 Use small input files (`input1.txt`, `input2.txt`) for testing. Run from terminal for clean output.

---

## 🧾 Example Output

```
Node 1 adjacency matrix:
 0   5.0  3.5  -1
5.0   0   -1  -1
3.5  -1    0  -1
-1   -1   -1   0
```

---

## 📌 Notes

- ✅ Allowed imports: `java.net.*`, `java.util.*`, `java.io.*`
- 🧵 Thread-safe code is required — use `synchronized` where needed.
- 🔒 No topology sharing between nodes outside of allowed socket messages.
- 🔁 All nodes should eventually converge to identical matrices.

---

## 🛠️ Potential Extensions

✨ Add a GUI for graph visualization  
📊 Implement logging or real-time message viewer  
⚙️ Simulate packet forwarding / routing failures
