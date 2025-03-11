import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Server class to handle incoming connections.
 */
public class Server {
  private final BlockingQueue<String> serverClientCommQueue;
  private final String hostname;
  private final int peerId;
  private final ServerSocket serverSocket;
  private final Membership membership;
  private final int port;
  private final String[] peerOrder;
  private final AtomicInteger leaderId;
  private final int crashDelay;
  private final Queue<String> reqLog;
  private final int expectedHearbeatInterval;

  public Server(BlockingQueue<String> serverClientCommQueue, String hostname, int peerId,
                Membership membership, int port, String[] peerOrder, AtomicInteger leaderId,
                int crashDelay, int heartbeatInterval) {
    this.serverClientCommQueue = serverClientCommQueue;
    this.hostname = hostname;
    this.peerId = peerId;
    try {
      this.serverSocket = new ServerSocket(port);
    } catch (IOException e) {
      throw new RuntimeException("Server error: " + e.getMessage());
    }
    this.membership = membership;
    this.port = port;
    this.peerOrder = peerOrder;
    this.leaderId = leaderId;
    this.crashDelay = crashDelay;
    this.reqLog = new ConcurrentLinkedQueue<>();
    this.expectedHearbeatInterval = heartbeatInterval;
  }

  public void start() {
    // start listening for heartbeat messages
    HeartbeatListener heartbeatListener = new HeartbeatListener(this.hostname, this.peerId,
            this.port, expectedHearbeatInterval, this.membership, this.peerOrder, this.leaderId);
    heartbeatListener.start();

    while (true) {
      // accept a connection
      Socket clientSocket;
      try {
        clientSocket = this.serverSocket.accept();
      } catch (IOException e) {
        throw new RuntimeException("Server accept error: " + e.getMessage());
      }

      // create a new thread to handle the connection
      Thread clientHandler = new Thread(new ClientHandler(this.serverClientCommQueue,
              this.hostname, this.peerId, clientSocket, this.membership, this.port, this.peerOrder,
              this.leaderId, this.crashDelay, this.reqLog));
      clientHandler.start();
    }
  }
}
