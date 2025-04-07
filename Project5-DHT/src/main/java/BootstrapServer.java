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
        Socket clientSocket = serverSocket.accept();
        new Thread(() -> handleConnection(clientSocket)).start();
      }
    } catch (IOException e) {
      throw new RuntimeException("Boostrap error: " + e.getMessage());
    }
  }

  /**
   * Handles each connection with a client. It reads the message from the peer and leaves it to
   * another helper method to handle the message.
   *
   * @param clientSocket the socket of the client
   */
  private void handleConnection(Socket clientSocket) {
    try (
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
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

    switch (msgRec.get("message_type")) {
      case "JOIN" -> handlePeerJoining(msgRec);
      case "PRED_ASSIGNED", "SUCC_ASSIGNED" -> {
        this.joinUpdateCount++;
        if (this.joinUpdateCount == 4) {
          broadcastNewPeerJoined();
          this.joinUpdateCount = 0;
        }
      }
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
      put("message_type", "REASSIGN_PREDECESSOR");
      put("new_id", pred_id);
    }});

    try (
            Socket socket = new Socket(peerId, Utils.PORT);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream())
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
      put("message_type", "REASSIGN_SUCCESSOR");
      put("new_id", succ_id);
    }});

    try (
            Socket socket = new Socket(peerId, Utils.PORT);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream())
    ) {
      out.writeUTF(msg);
    } catch (IOException e) {
      throw new RuntimeException("Boostrap error: " + e.getMessage());
    }

    return succ_id;
  }

  /**
   * Broadcasts a message to all peers in the ring indicating that a new peer has joined. The
   * entire ring is also printed to the console.
   */
  private void broadcastNewPeerJoined() {
    System.err.println("[" + String.join(" ", this.ring) + "]");

    for (String peer : this.ring) {
      try (
              Socket socket = new Socket(peer, Utils.PORT);
              DataOutputStream out = new DataOutputStream(socket.getOutputStream())
      ){
        String msg = Utils.prepareMsg(new LinkedHashMap<>() {{
          put("message_type", "NEW_PEER_JOINED");
        }});
        out.writeUTF(msg);
      } catch (IOException e) {
        throw new RuntimeException("Boostrap error: " + e.getMessage());
      }
    }
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
            Comparator.comparingInt(Utils::extractPeerIdNum));
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
