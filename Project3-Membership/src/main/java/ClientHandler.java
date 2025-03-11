import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientHandler implements Runnable {
  private BlockingQueue<String> serverClientCommQueue;
  private final String hostname;
  private final int peerId;
  private final Socket clientSocket;
  private final Membership membership;
  private final int port;
  private final String[] peerOrder;
  private final AtomicInteger leaderId;
  private final int crashDelay;
  private final String clientHostname;
  private final int clientId;
  private final DataInputStream in;
  private final DataOutputStream out;
  private final Queue<String> reqLog;

  public ClientHandler(BlockingQueue<String> serverClientCommQueue, String hostname, int peerId,
                       Socket clientSocket, Membership membership, int port, String[] peerOrder,
                       AtomicInteger leaderId, int crashDelay, Queue<String> reqLog) {
    this.serverClientCommQueue = serverClientCommQueue;
    this.hostname = hostname;
    this.peerId = peerId;
    this.clientSocket = clientSocket;
    this.membership = membership;
    this.port = port;
    this.peerOrder = peerOrder;
    this.leaderId = leaderId;
    this.crashDelay = crashDelay;
    this.clientHostname = Util.getHostname(this.clientSocket);

    int clientId = 0;
    for (int i = 0; i < this.peerOrder.length; i++) {
      if (this.peerOrder[i].equals(this.clientHostname)) {
        clientId = i + 1;
        break;
      }
    }
    if (clientId == 0) {
      throw new IllegalArgumentException("Client handler error: Hostname not found in peer order");
    }
    this.clientId = clientId;

    try {
      this.in = new DataInputStream(this.clientSocket.getInputStream());
      this.out = new DataOutputStream(this.clientSocket.getOutputStream());
    } catch (IOException e) {
      throw new RuntimeException("Client handler error: " + e.getMessage());
    }

    this.reqLog = reqLog;
  }

  @Override
  public void run() {
    while (true) {
      String msg;
      try {
        msg = this.in.readUTF();
      } catch (IOException e) {
        continue;
      }
      try {
        processMessage(msg);
      } catch (IOException e) {
        throw new RuntimeException("Client handler error: " + e.getMessage());
      }
    }
  }

  /**
   * Processes a message received from the client.
   *
   * @param msg the message to process
   */
  private void processMessage(String msg) throws IOException {
    String[] parts = msg.split(":");
    switch (parts[0]) {
      case "JOIN" -> {
        // no need to send REQ message if there aren't other peers apart from the leader
        if (this.membership.getPeers().size() > 1) {
          // broadcast REQ message
          Util.queueMessage(this.serverClientCommQueue, buildReqAddMessage());

          // wait for all OK messages from all other peers
          waitForAllOkays();
        }

        // update membership
        this.membership.addPeer(this.clientId);

        // broadcast NEWVIEW message
        Util.queueMessage(this.serverClientCommQueue, buildNewViewMessage());
      }

      case "REQ" -> {
        // save the operation that must be performed
        int requestId = Integer.parseInt(parts[1]);
        int viewId = Integer.parseInt(parts[2]);
        String operationType = parts[3];
        String reqMsg;

        if (operationType.equals("DEL")) {
          int deadPeerId = Integer.parseInt(parts[4]);
          reqMsg = "{request_id: " + requestId + ", view_id: " + viewId + ", operation_type: " +
                  operationType + "peer_id_to_remove: " + deadPeerId + "}";
        } else {
          reqMsg = "{request_id: " + requestId + ", view_id: " + viewId + ", operation_type: " +
                  operationType + "}";
        }

        this.reqLog.add(reqMsg);

        // send back an OK message containing the request id and the current view id
        out.writeUTF("OK:" + requestId + ":" + this.membership.getViewId());
      }

      case "NEWVIEW" -> {
        this.membership.setViewId(Integer.parseInt(parts[1]));
        this.membership.setPeers(Util.stringToList(parts[2]));
        System.err.println("{peer_id: " + this.peerId + ", view_id: " +
                this.membership.getViewId() + ", leader: " + this.leaderId.get() + ", peers: " +
                Util.listToString(this.membership.getPeers()) + "}");

        // if the crash delay is provided, crash, else the process will continue running
        crash();
      }

      case "DEADPEER" -> {
        int deadPeerId = Integer.parseInt(parts[1]);

        // broadcast REQ message
        Util.queueMessage(this.serverClientCommQueue, buildReqDelMessage(deadPeerId));

        // wait for all OK messages from all other peers
        waitForAllOkays();

        // sleep for a bit to let other processes recognize dead process
        Util.sleep(1000);

        // remove dead peer from membership
        this.membership.removePeer(deadPeerId);

        // broadcast NEWVIEW message
        Util.queueMessage(this.serverClientCommQueue, buildNewViewMessage());
      }

      default -> throw new IllegalArgumentException("Client handler error: Invalid message");
    }
  }

  /**
   *  Build a REQ message for ADD operation with the new request id, current view id, and operation
   *  type appended.
   *
   * @return a REQ message
   */
  private String buildReqAddMessage() {
    int requestId = this.reqLog.size() + 1;
    int viewId = this.membership.getViewId();
    String operationType = "ADD";

    this.reqLog.add("{request_id: " + requestId + ", view_id: " + viewId + ", operation_type: " +
            operationType + "}");
    return "REQ:" + requestId + ":" + viewId + ":" + operationType;
  }

  /**
   *  Build a REQ message for DEL operation with the new request id, current view id, operation
   *  type, and dead peer to be removed appended.
   *
   * @param deadPeerId the dead peer to be removed
   * @return a REQ message
   */
  private String buildReqDelMessage(int deadPeerId) {
    int requestId = this.reqLog.size() + 1;
    int viewId = this.membership.getViewId();
    String operationType = "DEL";

    this.reqLog.add("{request_id: " + requestId + ", view_id: " + viewId + ", operation_type: " +
            operationType + "peer_id_to_remove: " + deadPeerId + "}");
    return "REQ:" + requestId + ":" + viewId + ":" + operationType + ":" + deadPeerId;
  }

  /**
   * Build a NEWVIEW message with the current view id and peers appended to the command.
   *
   * @return a NEWVIEW message
   */
  private String buildNewViewMessage() {
    return "NEWVIEW:" + this.membership.getViewId() + ":" +  this.membership.getPeers();
  }

  /**
   * Wait for all OK messages from all other peers. When all other peers have sent the OK message
   * to the client, the client will let the server side know.
   */
  private void waitForAllOkays() {
    String confirmationMsg = this.serverClientCommQueue.peek();
    while (confirmationMsg == null || !confirmationMsg.equals("ToServer:OK")) {
      confirmationMsg = this.serverClientCommQueue.peek();
    }
    Util.dequeueMessage(this.serverClientCommQueue);
  }

  /**
   * Crash process if the crash delay is provided. If not, nothing happens.
   */
  private void crash() {
    if (this.crashDelay != -1) {
      Util.sleep(this.crashDelay * 1000);
      System.err.println("{peer_id: " + this.peerId + ", view_id: " +
              this.membership.getViewId() + ", leader: " + this.leaderId.get() +
              ", message:\"crashing\"}");
      System.exit(0);
    }
  }
}
