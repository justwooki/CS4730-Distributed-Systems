import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientHandler implements Runnable {
  private BlockingQueue<String> queue;
  private final String hostname;
  private final int peerId;
  private final Socket clientSocket;
  private final Membership membership;
  private final int port;
  private final String[] peerOrder;
  private final AtomicInteger leaderId;
  private final String clientHostname;
  private final int clientId;
  private final DataInputStream in;
  private final DataOutputStream out;
  private final List<RequestMessage> reqLog;

  public ClientHandler(BlockingQueue<String> queue, String hostname, int peerId,
                       Socket clientSocket, Membership membership, int port, String[] peerOrder,
                       AtomicInteger leaderId) {
    this.queue = queue;
    this.hostname = hostname;
    this.peerId = peerId;
    this.clientSocket = clientSocket;
    this.membership = membership;
    this.port = port;
    this.peerOrder = peerOrder;
    this.leaderId = leaderId;
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

    this.reqLog = new ArrayList<>();
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
        Util.queueMessage(this.queue, buildReqMessage()); // broadcast REQ message
        waitForAllOkays(); // wait for all OK messages from all other peers
        this.membership.addPeer(this.clientId); // update membership
        Util.queueMessage(this.queue, buildNewViewMessage()); // broadcast NEWVIEW message
      }
      case "REQ" -> {
        // save the operation that must be performed
        int requestId = Integer.parseInt(parts[1]);
        int viewId = Integer.parseInt(parts[2]);
        String operationType = parts[3];
        RequestMessage reqMsg = new RequestMessage(requestId, viewId, operationType);
        this.reqLog.add(reqMsg);

        // send back an OK message containing the request id and the current view id
        out.writeUTF("OK:" + requestId + ":" + this.membership.getViewId());
      }
      case "NEWVIEW" -> {
        this.membership.setViewId(Integer.parseInt(parts[1]));
        this.membership.setPeers(Util.stringToList(parts[2]));
        System.err.println("{peer_id: " + this.peerId + ", view_id: " +
                this.membership.getViewId() + ", peers: " +
                Util.listToString(this.membership.getPeers()) + "}");
      }
      default -> throw new IllegalArgumentException("Client handler error: Invalid message");
    }
  }

  /**
   * Build a REQ message with the new request id, current view id, and operation type appended
   * to the command.
   *
   * @return a REQ message
   */
  private String buildReqMessage() {
    int requestId = this.reqLog.size() + 1;
    int viewId = this.membership.getViewId();
    String operationType = "ADD";
    return "REQ:" + requestId + ":" + viewId + ":" + operationType;
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
    String confirmationMsg = this.queue.peek();
    while (confirmationMsg == null || !confirmationMsg.equals("ToServer:OK")) {
      confirmationMsg = this.queue.peek();
    }
    Util.dequeueMessage(this.queue);
  }
}
