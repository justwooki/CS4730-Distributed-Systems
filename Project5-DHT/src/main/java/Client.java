package main.java;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * A client stores and retrieves objects from the DHT. It can only communicate with the bootstrap
 * server. The bootstrap server will let the client know when the object is successfully stored or
 * retrieved.
 */
public final class Client {
  private final String clientId;
  private final String bootstrapServerName;
  private final int delay;
  private final int objectId;
  private final Action action;
  private int requestId = 0;

  /**
   * Constructs a new Client object.
   *
   * @param clientId the unique ID of the client
   * @param bootstrapServerName the hostname of the bootstrap server
   * @param delay the number of seconds to wait before starting the client
   * @param objectId the ID of the object to be stored or retrieved
   * @param action the action to be performed (STORE or RETRIEVE)
   */
  public Client(String clientId, String bootstrapServerName, int delay, int objectId,
                String action) {
    this.clientId = clientId;
    this.bootstrapServerName = bootstrapServerName;
    this.delay = delay;
    this.objectId = objectId;
    this.action = Action.valueOf(action);
  }

  /**
   * An enum representing the actions that the client can perform: STORE or RETRIEVE.
   */
  private enum Action {
    STORE("STORE"),
    RETRIEVE("RETRIEVE");

    private final String actionName;

    /**
     * Constructs a new Action object.
     *
     * @param actionName the name of the action
     */
    Action(String actionName) {
      this.actionName = actionName;
    }

    /**
     * Returns the name of the action.
     *
     * @return the name of the action
     */
    public String getActionName() {
      return this.actionName;
    }
  }

  /**
   * Starts the client process.
   */
  public void start() {
    try {
      Thread.sleep(this.delay * 1000L);
    } catch (InterruptedException e) {
      throw new RuntimeException("Peer error: " + e.getMessage());
    }

    sendRequest();

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
   * Sends a request to the bootstrap server to store or retrieve an object.
   */
  private void sendRequest() {
    this.requestId++;
    String msg = Utils.prepareMsg(new LinkedHashMap<>(){{
      put("req_id", String.valueOf(requestId));
      put("operation_type", action.getActionName());
      put("object_id", String.valueOf(objectId));
      put("client_id", clientId);
    }});

    try (
            Socket bootstrapSocket = new Socket(this.bootstrapServerName, Utils.PORT);
            DataOutputStream out = new DataOutputStream(bootstrapSocket.getOutputStream())
    ) {
      out.writeUTF(msg);
    } catch (IOException e) {
      throw new RuntimeException("Client error: " + e.getMessage());
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
      throw new RuntimeException("Client error: " + e.getMessage());
    }
  }

  /**
   * Handles a message received from the bootstrap server appropriately depending on its contents.
   *
   * @param msg the message received from the bootstrap server
   * @throws IllegalArgumentException if the message is invalid
   */
  private void handleMessage(String msg) throws IllegalArgumentException {
    Map<String, String> msgRec = Utils.unpackMsg(msg);
    if (!this.clientId.equals(msgRec.get("client_id"))
            || this.objectId != Integer.parseInt(msgRec.get("object_id"))) {
      throw new IllegalArgumentException(
              "Client error: Invalid message received - client/object do not match");
    }

    switch (msgRec.get("operation_type")) {
      case "OBJ_STORED" -> System.err.println("STORED " + this.objectId);
      case "OBJ_RETRIEVED" -> System.err.println("RETRIEVED " + this.objectId);
      case "OBJ_NOT_FOUND" -> System.err.println("NOT FOUND " + this.objectId);
      default -> throw new RuntimeException("Peer error: Unknown message type received");
    }
  }

  /**
   * Main method to run the client.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    Client client = constructClient(args);
    client.start();
  }

  /**
   * Constructs and returns a new client based on the given arguments.
   *
   * @param args the given arguments that hold the properties of the client
   * @return a new client
   * @throws IllegalArgumentException if any arguments are invalid
   */
  private static Client constructClient(String[] args) throws IllegalArgumentException {
    String clientId;
    String bootstrapServerName = null;
    int testcase = -1;
    int delay = 0;
    int objectId;
    String action;

    // read arguments
    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "-b" -> {
          if (i + 1 < args.length) {
            bootstrapServerName = args[++i];
          } else {
            throw new IllegalArgumentException("Client error: Missing bootstrap server name");
          }
        }
        case "-t" -> {
          if (i + 1 < args.length) {
            testcase = Integer.parseInt(args[++i]);
          } else {
            throw new IllegalArgumentException("Client error: Missing testcase");
          }
        }
        case "-d" -> {
          if (i + 1 < args.length) {
            delay = Integer.parseInt(args[++i]);
          } else {
            throw new IllegalArgumentException("Client error: Missing delay");
          }
        }
        default -> throw new IllegalArgumentException("Client error: Invalid argument");
      }
    }

    // get client id
    try {
      int lowestClientId = 1;
      int highestClientId = 3;
      clientId = InetAddress.getLocalHost().getHostName() +
              new Random().nextInt(lowestClientId, highestClientId + 1);
    } catch (UnknownHostException e) {
      throw new RuntimeException("Client error: Unable to determine hostname: " + e.getMessage());
    }

    // get object id and action from testcase
    List<Integer> objIds = getObjIds(clientId);
    if (objIds.isEmpty()) {
      throw new IllegalArgumentException("Client error: No object IDs found for client ID: " +
              clientId);
    }
    int lowerBound = 1;
    int upperBound = 127;

    switch (testcase) {
      case 3 -> {
        objectId = new Random().nextInt(lowerBound, upperBound + 1);
        action = "STORE";
      }
      case 4 -> {
        objectId = objIds.get(new Random().nextInt(objIds.size()));
        action = "RETRIEVE";
      }
      case 5 -> {
        List<Integer> unretrievableObjects = IntStream.rangeClosed(lowerBound, upperBound)
                .filter(i -> !objIds.contains(i))
                .boxed()
                .toList();
        objectId = unretrievableObjects.get(new Random().nextInt(unretrievableObjects.size()));
        action = "RETRIEVE";
      }
      default -> throw new IllegalArgumentException("Client error: Invalid testcase");
    }

    return new Client(clientId, bootstrapServerName, delay, objectId, action);
  }

  /**
   * Retrieves the object IDs associated with the client.
   *
   * @param clientId the ID of the client
   * @return a list of object IDs associated with the client
   */
  private static List<Integer> getObjIds(String clientId) {
    List<Integer> objIds = new ArrayList<>();
    File dir = new File("/app/dockerfile-things/");
    File[] objFiles = dir.listFiles((d, name) -> name.matches("^objects\\d+\\.txt"));
    if (objFiles == null) {
      throw new RuntimeException("Client error: No object files found");
    }

    for (File objFile : objFiles) {
      try {
        List<Integer> fileObjIds = Files.readAllLines(objFile.toPath()).stream()
                .map(String::trim)
                .filter(line -> line.startsWith(String.valueOf(Utils.extractIdNum(clientId))))
                .map(line -> Integer.parseInt(line.split("::")[1].trim()))
                .toList();
        objIds.addAll(fileObjIds);
      } catch (IOException e) {
        throw new RuntimeException("Client error: Unable to read object file: " + e.getMessage());
      }
    }

    return objIds;
  }
}
