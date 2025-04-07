package main.java;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
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
            Socket bootstrapSocket = new Socket(bootstrapServerName, Utils.PORT);
            DataOutputStream out = new DataOutputStream(bootstrapSocket.getOutputStream());
    ) {
      String msg = Utils.prepareMsg(new LinkedHashMap<>() {{
        put("peer_id", peerId);
        put("operation_type", "JOIN");
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

    switch (msgRec.get("operation_type")) {
      case "REASSIGN_PREDECESSOR" -> reassignPredecessor(msgRec);
      case "REASSIGN_SUCCESSOR" -> reassignSuccessor(msgRec);
      case "NEW_PEER_JOINED" -> printPredAndSucc();
      case "STORE" -> storeObject(msg);
      case "RETRIEVE" -> retrieveObject(msg);
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
      put("operation_type", "PRED_ASSIGNED");
    }});

    try (
            Socket bootstrapSocket = new Socket(this.bootstrapServerName, Utils.PORT);
            DataOutputStream out = new DataOutputStream(bootstrapSocket.getOutputStream())
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
      put("operation_type", "SUCC_ASSIGNED");
    }});

    try (
            Socket bootstrapSocket = new Socket(this.bootstrapServerName, Utils.PORT);
            DataOutputStream out = new DataOutputStream(bootstrapSocket.getOutputStream())
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
   * Stores the object in the object file. If the object is not here, it passes the request to
   * the successor. After storing the object, it will notify the bootstrap server that the object
   * has been successfully stored.
   *
   * @param msg the message to be passed
   */
  private void storeObject(String msg) {
    Map<String, String> msgRec = Utils.unpackMsg(msg);
    String objId = msgRec.get("object_id");

    // pass to successor if object does not belong in this peer
    if (passObjToSuccIfNotHere(msg, objId)) {
      return;
    }

    String clientId = msgRec.get("client_id");
    String objStorageStr = Utils.extractIdNum(clientId) + "::" + objId;

    // store the object in the object file
    try (BufferedWriter writer = new BufferedWriter(
            new FileWriter(this.objFilePath, true))) {
      writer.write(objStorageStr);
      writer.newLine();
    } catch (IOException e) {
      throw new RuntimeException("Peer error: " + e.getMessage());
    }

    // print the object file
    StringBuilder objFileContent = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(
            new FileReader(this.objFilePath))) {
      for (String line; (line = reader.readLine()) != null;) {
        objFileContent.append(line).append("\n");
      }
    } catch (IOException e) {
      throw new RuntimeException("Peer error: " + e.getMessage());
    }
    System.err.println(objFileContent.toString().trim());

    // send message to bootstrap server
    try (
            Socket bootstrapSocket = new Socket(this.bootstrapServerName, Utils.PORT);
            DataOutputStream out = new DataOutputStream(bootstrapSocket.getOutputStream())
    ) {
      String newMsg = Utils.prepareMsg(new LinkedHashMap<>() {{
        put("operation_type", "OBJ_STORED");
        put("object_id", objId);
        put("client_id", clientId);
        put("peer_id", peerId);
      }});
      out.writeUTF(newMsg);
    } catch (IOException e) {
      throw new RuntimeException("Peer error: " + e.getMessage());
    }
  }

  /**
   * Retrieves the object from the object file. If the object is not here, it passes the request to
   * the successor. After retrieving the object, it will notify the bootstrap server that the
   * object has been successfully retrieved.
   *
   * @param msg the message to be passed
   */
  private void retrieveObject(String msg) {
    Map<String, String> msgRec = Utils.unpackMsg(msg);
    String objId = msgRec.get("object_id");

    // pass to successor if object does not belong in this peer
    if (passObjToSuccIfNotHere(msg, objId)) {
      return;
    }

    String clientId = msgRec.get("client_id");

    try {
      // look for the object in the object file
      List<String> objList = Files.readAllLines(Paths.get(this.objFilePath)).stream()
              .map(line -> line.split("::"))
              .filter(line -> line[0].equals(String.valueOf(Utils.extractIdNum(clientId))))
              .filter(line -> line[1].equals(objId))
              .map(line -> line[1]).toList();

      // send message to bootstrap server
      Socket bootstrapSocket = new Socket(this.bootstrapServerName, Utils.PORT);
      DataOutputStream out = new DataOutputStream(bootstrapSocket.getOutputStream());

      String newMsg;
      if (objList.isEmpty()) {
        newMsg = Utils.prepareMsg(new LinkedHashMap<>() {{
          put("operation_type", "OBJ_NOT_FOUND");
          put("object_id", objId);
          put("client_id", clientId);
        }});
      } else {
        newMsg = Utils.prepareMsg(new LinkedHashMap<>() {{
          put("operation_type", "OBJ_RETRIEVED");
          put("object_id", objId);
          put("client_id", clientId);
          put("peer_id", peerId);
        }});
      }

      out.writeUTF(newMsg);
      out.close();
      bootstrapSocket.close();
    } catch (IOException e) {
      throw new RuntimeException("Peer error: " + e.getMessage());
    }
  }

  /**
   * Passes the object to the successor if it is not here. Returns true if the object was passed,
   * false otherwise.
   *
   * @param msg the message to be passed
   * @param objId the object ID
   * @return true if the object was passed, false otherwise
   */
  private boolean passObjToSuccIfNotHere(String msg, String objId) {
    boolean objIdLargerThanPeerId = Utils.extractIdNum(objId)
            > Utils.extractIdNum(this.peerId);
    boolean atStartOfRing = Utils.extractIdNum(this.predecessorId)
            > Utils.extractIdNum(this.peerId);
    boolean objIdLargerThanPredecessorId = Utils.extractIdNum(objId)
            > Utils.extractIdNum(this.predecessorId);
    boolean objIdLargerThanRing = atStartOfRing && objIdLargerThanPredecessorId;

    if (objIdLargerThanPeerId && !objIdLargerThanRing) {
      try (
              Socket succSocket = new Socket(this.successorId, Utils.PORT);
              DataOutputStream out = new DataOutputStream(succSocket.getOutputStream())
      ) {
        out.writeUTF(msg);
        return true;
      } catch (IOException e) {
        throw new RuntimeException("Peer error: " + e.getMessage());
      }
    }

    return false;
  }

  /**
   * Main method to start the peer process.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    Peer peer = constructPeer(args);
    peer.start();
  }

  /**
   * Constructs and returns a new peer based on the given arguments.
   *
   * @param args the given arguments that hold the properties of the peer
   * @return a new peer
   * @throws IllegalArgumentException if any arguments are invalid
   */
  private static Peer constructPeer(String[] args) throws IllegalArgumentException {
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

    return new Peer(peerId, bootstrapServerName, objFilePath, delay);
  }
}
