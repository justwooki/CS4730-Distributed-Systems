package main.java;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a peer in a ring. Each peer maintains a predecessor and successor peer in the ring,
 * and a file with stored objects. Each peer starts by contacting the bootstrap server, which will
 * tell each peer their place in the ring. As new peers join, the boostrap server may inform the
 * peers to update their predecessor and successor. Peers may get a request to store or retrieve
 * an object in or from the object file. If the object to be stored or retrieved does not belong
 * to the peer, the peer will forward the request to its successor in the ring.
 */
public final class Peer {
  private final String peerId;
  private final String bootstrapServerName;
  private final String objFilePath;
  private final int delay;
  private String predecessorId;
  private String successorId;

  /**
   * Constructs a new Peer object. Each peer has its unique ID, a predecessor, and successor.
   *
   * @param peerId the unique ID of the peer
   * @param bootstrapServerName the hostname of the bootstrap server
   * @param objFilePath path to a file containing the object store of the peer
   * @param delay the number of seconds to wait before joining after startup
   */
  public Peer(String peerId, String bootstrapServerName, String objFilePath, int delay) {
    this.peerId = peerId;
    this.bootstrapServerName = bootstrapServerName;
    this.objFilePath = objFilePath;
    this.delay = delay;
    this.predecessorId = null;
    this.successorId = null;
  }

  /**
   * Starts the peer process.
   */
  public void start() {
    try {
      Thread.sleep(delay * 1000L);
    } catch (InterruptedException e) {
      throw new RuntimeException("Peer error: " + e.getMessage());
    }

    joinRing();

    try (ServerSocket socket = new ServerSocket(Utils.PORT)) {
      while (true) {
        Socket bootstrapSocket = socket.accept();
        new Thread(() -> handleConnection(bootstrapSocket)).start();
      }
    } catch (IOException e) {
      throw new RuntimeException("Acceptor error: " + e.getMessage());
    }
  }

  /**
   * Contacts the bootstrap server to join the ring.
   */
  private void joinRing() {
    try (
            Socket socket = new Socket(bootstrapServerName, Utils.PORT);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
    ) {
      String msg = Utils.prepareMsg(new LinkedHashMap<>() {{
        put("peer_id", peerId);
        put("message_type", "JOIN");
      }});
      out.writeUTF(msg);
    } catch (Exception e) {
      throw new RuntimeException("Peer error: " + e.getMessage());
    }
  }

  /**
   * Handles each connection with the bootstrap server. It reads the message from the bootstrap
   * server and leaves it to another helper method to handle the message.
   *
   * @param bootstrapSocket the socket of the bootstrap server
   */
  private void handleConnection(Socket bootstrapSocket) {
    try (
            DataInputStream in = new DataInputStream(bootstrapSocket.getInputStream());
    ) {
      String msg = in.readUTF();
      handleMessage(msg);
    } catch (IOException e) {
      throw new RuntimeException("Peer error: " + e.getMessage());
    }

  }

  /**
   * Handles a message received from the bootstrap server appropriately depending on its contents.
   *
   * @param msg the message received from the bootstrap server
   */
  private void handleMessage(String msg) {
    Map<String, String> msgRec = Utils.unpackMsg(msg);

    switch (msgRec.get("message_type")) {
      case "REASSIGN_PREDECESSOR" -> {
        reassignPredecessor(msgRec);
      }
      case "REASSIGN_SUCCESSOR" -> {
        reassignSuccessor(msgRec);
      }
      case "NEW_PEER_JOINED" -> {
        printPredAndSucc();
      }
      default -> throw new RuntimeException("Peer error: Unknown message type received");
    }
  }

  /**
   * Reassigns the predecessor of the peer based on the message received from the bootstrap server
   * and notifies the bootstrap server.
   *
   * @param msg the message received from the bootstrap server
   */
  private void reassignPredecessor(Map<String, String> msg) {
    this.predecessorId = msg.get("new_id");
    String newMsg = Utils.prepareMsg(new LinkedHashMap<>() {{
      put("peer_id", peerId);
      put("message_type", "PRED_ASSIGNED");
    }});

    try (
            Socket socket = new Socket(this.bootstrapServerName, Utils.PORT);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream())
    ) {
      out.writeUTF(newMsg);
    } catch (IOException e) {
      throw new RuntimeException("Peer error: " + e.getMessage());
    }
  }

  /**
   * Reassigns the successor of the peer based on the message received from the bootstrap server
   * and notifies the bootstrap server.
   *
   * @param msg the message received from the bootstrap server
   */
  private void reassignSuccessor(Map<String, String> msg) {
    this.successorId = msg.get("new_id");

    String newMsg = Utils.prepareMsg(new LinkedHashMap<>() {{
      put("peer_id", peerId);
      put("message_type", "SUCC_ASSIGNED");
    }});

    try (
            Socket socket = new Socket(this.bootstrapServerName, Utils.PORT);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream())
    ) {
      out.writeUTF(newMsg);
    } catch (IOException e) {
      throw new RuntimeException("Peer error: " + e.getMessage());
    }
  }

  /**
   * Prints the predecessor and successor of the peer.
   */
  private void printPredAndSucc() {
    System.err.println("Predecessor: " + this.predecessorId + ", Successor: " + this.successorId);
  }

  /**
   * Main method to start the peer process.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    String peerId;
    String bootstrapServerName = null;
    String objFilePath = null;
    int delay = 0;

    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "-b" -> {
          if (i + 1 < args.length) {
            bootstrapServerName = args[++i];
          } else {
            throw new IllegalArgumentException("Peer error: Missing bootstrap server name");
          }
        }
        case "-o" -> {
          if (i + 1 < args.length) {
            objFilePath = args[++i];
          } else {
            throw new IllegalArgumentException("Peer error: Missing object file path");
          }
        }
        case "-d" -> {
          if (i + 1 < args.length) {
            delay = Integer.parseInt(args[++i]);
          } else {
            throw new IllegalArgumentException("Peer error: Missing delay");
          }
        }
        default -> throw new IllegalArgumentException("Peer error: Invalid argument");
      }
    }

    try {
      peerId = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      throw new RuntimeException("Peer error: Unable to determine hostname: " + e.getMessage());
    }

    Peer peer = new Peer(peerId, bootstrapServerName, objFilePath, delay);
    peer.start();
  }
}
