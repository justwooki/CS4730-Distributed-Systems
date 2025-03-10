import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Client {
  BlockingQueue<String> queue;
  private final String hostname;
  private final int peerId;
  private final Membership membership;
  private final int port;
  private final String[] peerOrder;
  private final AtomicInteger leaderId;

  public Client(BlockingQueue<String> queue, String hostname, int peerId, Membership membership,
                int port, String[] peerOrder, AtomicInteger leaderId) {
    this.queue = queue;
    this.hostname = hostname;
    this.peerId = peerId;
    this.membership = membership;
    this.port = port;
    this.peerOrder = peerOrder;
    this.leaderId = leaderId;
  }

  public void start() {
    try {
      Socket socket = new Socket(this.peerOrder[this.leaderId.get() - 1], this.port);
      DataOutputStream out = new DataOutputStream(socket.getOutputStream());

      // start sending heartbeat messages
      int heartbeatInterval = 5;
      HeartbeatSender heartbeatSender = new HeartbeatSender(this.port, heartbeatInterval);
      heartbeatSender.start();

      out.writeUTF("JOIN");

      while (true) {
        String msg = this.queue.peek();
        if (msg != null && !msg.startsWith("ToServer:")) {
          Util.dequeueMessage(this.queue);
          processMessage(msg);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Client error: " + e.getMessage());
    }
  }

  /**
   * Process a message from the queue.
   *
   * @param msg the message to process
   * @throws IOException if an I/O error occurs
   */
  private void processMessage(String msg) throws IOException {
    String[] parts = msg.split(":");
    switch (parts[0]) {
      case "REQ" -> {
        broadcastReq(msg); // broadcast REQ message and receive OK messages

        // let server know all OK messages are have been received
        Util.queueMessage(this.queue, "ToServer:OK");
      }
      case "NEWVIEW" -> broadcastNewView(msg);
      default -> throw new IllegalArgumentException("Client error: Invalid message");
    }
  }

  /**
   * Broadcast NEWVIEW message to all members.
   *
   * @param msg the message to broadcast
   * @throws IOException if an I/O error occurs
   */
  private void broadcastNewView(String msg) throws IOException {
    List<Integer> allMembers = this.membership.getPeers();
    for (Integer i : allMembers) {
      String memberHostname = this.peerOrder[i - 1];
      Socket memberSocket = new Socket(memberHostname, this.port);
      DataOutputStream out = new DataOutputStream(memberSocket.getOutputStream());

      out.writeUTF(msg);

      out.close();
      memberSocket.close();
    }
  }

  /**
   * Broadcast REQ message to all members (except leader).
   *
   * @param msg the message to broadcast
   * @throws IOException if an I/O error occurs
   */
  private void broadcastReq(String msg) throws IOException {
    List<Integer> allMembers = this.membership.getPeers();
    String[] parts = msg.split(":");
    int requestId = Integer.parseInt(parts[1]);
    int viewId = Integer.parseInt(parts[2]);

    for (Integer i : allMembers) {
      if (i == this.peerId) {
        continue;
      }

      String memberHostname = this.peerOrder[i - 1];
      Socket memberSocket = new Socket(memberHostname, this.port);
      DataInputStream in = new DataInputStream(memberSocket.getInputStream());
      DataOutputStream out = new DataOutputStream(memberSocket.getOutputStream());

      out.writeUTF(msg); // send REQ message
      String[] response = in.readUTF().split(":"); // receive OK message

      if (!response[0].equals("OK")) { // check if response is OK
        throw new RuntimeException("Client error: Message is not \"OK\"");
      } else if (requestId != Integer.parseInt(response[1])) { // check if request ID matches
        throw new RuntimeException("Client error: Mismatching request ID");
      } else if (viewId != Integer.parseInt(response[2])) { // check if view ID matches
        throw new RuntimeException("Client error: Mismatching view ID");
      }

      out.close();
      memberSocket.close();
    }
  }
}
