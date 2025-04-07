package main.java;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The bootstrap server is responsible for maintaining the ring as peers join and telling each peer
 * where their place is in the ring. It will inform each peer from the ring that need to update
 * their predecessor and successor. It doesn't monitor the peers. A client can interact with the
 * boostrap server to store and retrieve objects in the ring. When receiving such a request, the
 * boostrap server will forward the request to the first peer in the ring.
 */
public final class BootstrapServer {
  private final List<String> ring;
  private int joinUpdateCount = 0;

  /**
   * Constructs a new BootstrapServer object. A bootstrap server has a list of peers in a sorted
   * ring.
   */
  public BootstrapServer() {
    this.ring = new ArrayList<>();
  }

  /**
   * Starts the bootstrap server, which listens for incoming connections from peers.
   */
  public void start() {
    try (ServerSocket serverSocket = new ServerSocket(Utils.PORT)) {
      while (true) {
        Socket socket = serverSocket.accept();
        new Thread(() -> handleConnection(socket)).start();
      }
    } catch (IOException e) {
      throw new RuntimeException("Boostrap error: " + e.getMessage());
    }
  }

  /**
   * Handles each connection. It reads the message and leaves it to another helper method to handle
   * it.
   *
   * @param socket the socket
   */
  private void handleConnection(Socket socket) {
    try (
            DataInputStream in = new DataInputStream(socket.getInputStream());
    ) {
      String msg = in.readUTF();
      handleMessage(msg);
    } catch (IOException e) {
      throw new RuntimeException("Boostrap error: " + e.getMessage());
    }
  }

  /**
   * Handles a message received from a peer appropriately depending on its contents.
   *
   * @param msg the message received from the peer
   */
  private void handleMessage(String msg) {
    Map<String, String> msgRec = Utils.unpackMsg(msg);

    switch (msgRec.get("operation_type")) {
      case "JOIN" -> handlePeerJoining(msgRec);
      case "PRED_ASSIGNED", "SUCC_ASSIGNED" -> {
        this.joinUpdateCount++;
        int numPeersThatWillUpdate = 4;
        if (this.joinUpdateCount == numPeersThatWillUpdate) {
          broadcastNewPeerJoined();
          this.joinUpdateCount = 0;
        }
      }
      case "STORE", "RETRIEVE" -> handleClientRequest(msg);
      case "OBJ_STORED", "OBJ_RETRIEVED", "OBJ_NOT_FOUND" ->
              handleReportToClient(msg, msgRec.get("client_id"));
      default -> throw new RuntimeException("Boostrap error: unknown message type");
    }
  }

  /**
   * Handles a peer joining the ring. It adds the peer to the ring and makes appropriate
   * adjustments to its predecessor and successor.
   *
   * @param msgRec the message received from the peer telling the server that it is joining
   */
  private void handlePeerJoining(Map<String, String> msgRec) {
    String peerId = msgRec.get("peer_id");
    addPeerToRing(peerId);
    String pred = assignPred(peerId);
    String succ = assignSucc(peerId);
    assignSucc(pred);
    assignPred(succ);
  }

  /**
   * Adds a peer to the ring in a sorted manner.
   *
   * @param peerId the ID of the peer to add
   */
  private void addPeerToRing(String peerId) {
    int insertionPoint = binarySearch(peerId);
    if (insertionPoint >= 0) {
      throw new RuntimeException("Boostrap error: cannot add duplicate peer to ring");
    }
    insertionPoint = -insertionPoint - 1;
    this.ring.add(insertionPoint, peerId);
  }

  /**
   * Sends a message to the given peer to assign a new predecessor.
   *
   * @param peerId id of the peer to assign predecessor
   * @return the id of the predecessor
   */
  private String assignPred(String peerId) {
    int peerIdx = binarySearch(peerId);
    if (peerIdx < 0) {
      throw new RuntimeException("Boostrap error: peer doesn't exist in ring");
    }

    String pred_id = this.ring.get((peerIdx == 0) ? this.ring.size() - 1 : peerIdx - 1);
    String msg = Utils.prepareMsg(new LinkedHashMap<>() {{
      put("peer_id", peerId);
      put("operation_type", "REASSIGN_PREDECESSOR");
      put("new_id", pred_id);
    }});

    try (
            Socket peerSocket = new Socket(peerId, Utils.PORT);
            DataOutputStream out = new DataOutputStream(peerSocket.getOutputStream())
    ) {
      out.writeUTF(msg);
    } catch (IOException e) {
      throw new RuntimeException("Boostrap error: " + e.getMessage());
    }

    return pred_id;
  }

  /**
   * Sends a message to the given peer to assign a new successor.
   *
   * @param peerId id of the peer to assign successor
   * @return the id of the successor
   */
  private String assignSucc(String peerId) {
    int peerIdx = binarySearch(peerId);
    if (peerIdx < 0) {
      throw new RuntimeException("Boostrap error: peer doesn't exist in ring");
    }

    String succ_id = this.ring.get((peerIdx == this.ring.size() - 1) ? 0 : peerIdx + 1);
    String msg = Utils.prepareMsg(new LinkedHashMap<>() {{
      put("peer_id", peerId);
      put("operation_type", "REASSIGN_SUCCESSOR");
      put("new_id", succ_id);
    }});

    try (
            Socket peerSocket = new Socket(peerId, Utils.PORT);
            DataOutputStream out = new DataOutputStream(peerSocket.getOutputStream())
    ) {
      out.writeUTF(msg);
    } catch (IOException e) {
      throw new RuntimeException("Boostrap error: " + e.getMessage());
    }

    return succ_id;
  }

  /**
   * Searches for a peer in the sorted ring by comparing the numeric parts of their ID.
   *
   * @param peerId the peer ID to search for
   * @return the index of the peer, if found; otherwise (-insertionPoint - 1) where insertionPoint
   *         is the index it would've been inserted in
   */
  private int binarySearch(String peerId) {
    return Collections.binarySearch(this.ring, peerId,
            Comparator.comparingInt(Utils::extractIdNum));
  }

  /**
   * Broadcasts a message to all peers in the ring indicating that a new peer has joined. The
   * entire ring is also printed to the console.
   */
  private void broadcastNewPeerJoined() {
    System.err.println("[" + String.join(" ", this.ring) + "]");

    for (String peer : this.ring) {
      try (
              Socket peerSocket = new Socket(peer, Utils.PORT);
              DataOutputStream out = new DataOutputStream(peerSocket.getOutputStream())
      ){
        String msg = Utils.prepareMsg(new LinkedHashMap<>() {{
          put("operation_type", "NEW_PEER_JOINED");
        }});
        out.writeUTF(msg);
      } catch (IOException e) {
        throw new RuntimeException("Boostrap error: " + e.getMessage());
      }
    }
  }

  /**
   * Handles a client request to store or retrieve an object. It forwards the request to the first
   * peer on the ring.
   *
   * @param msg the message received from the client to be forwarded to the first peer
   */
  private void handleClientRequest(String msg) {
    try (
            Socket peerSocket = new Socket(this.ring.get(0), Utils.PORT);
            DataOutputStream out = new DataOutputStream(peerSocket.getOutputStream())
    ) {
      out.writeUTF(msg);
    } catch (IOException e) {
      throw new RuntimeException("Boostrap error: " + e.getMessage());
    }
  }

  /**
   * Handles a message received from a peer indicating that a client request has been completed. It
   * forwards the message to the client.
   *
   * @param msg the message received from the peer
   * @param clientId the ID of the client to whom the message should be forwarded
   */
  private void handleReportToClient(String msg, String clientId) {
    try (
            Socket clientSocket = new Socket(Utils.extractHostname(clientId), Utils.PORT);
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())
    ) {
      out.writeUTF(msg);
    } catch (IOException e) {
      throw new RuntimeException("Boostrap error: " + e.getMessage());
    }
  }

  /**
   * Main method to start the bootstrap server.
   *
   * @param args command line arguments (not used)
   */
  public static void main(String[] args) {
    BootstrapServer bootstrapServer = new BootstrapServer();
    bootstrapServer.start();
  }
}
