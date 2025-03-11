import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Initializes TCP server that listens for incoming client connections. Each connected client is
 * handled by a separate client handler running in its own thread. This way, the server is able to
 * handle multiple clients simultaneously with a multithreaded program.
 */
public class Server {
  private final BlockingQueue<String> serverClientCommQueue;
  private final String hostname;
  private final int peerId;
  private final Membership membership;
  private final int port;
  private final String[] peerOrder;
  private final AtomicInteger leaderId;
  private final int crashDelay;
  private final int expectedHearbeatInterval;

  /**
   * Constructs a Server.
   *
   * @param serverClientCommQueue the queue that allows the server side of the peer to communicate
   *                              with its client counterpart
   * @param hostname the hostname of the local peer
   * @param peerId the id of the local peer
   * @param membership the membership that stores current view id and all alive peers
   * @param port the port on which to establish a connection on
   * @param peerOrder the order of the peers in the system
   * @param leaderId the id of the leader process
   * @param crashDelay a sleep that should start immediately after sending an JOIN message; when
   *                   the sleep ends, the peer should crash (exit)
   * @param heartbeatInterval the interval (in seconds) between each heartbeat
   */
  public Server(BlockingQueue<String> serverClientCommQueue, String hostname, int peerId,
                Membership membership, int port, String[] peerOrder, AtomicInteger leaderId,
                int crashDelay, int heartbeatInterval) {
    this.serverClientCommQueue = serverClientCommQueue;
    this.hostname = hostname;
    this.peerId = peerId;
    this.membership = membership;
    this.port = port;
    this.peerOrder = peerOrder;
    this.leaderId = leaderId;
    this.crashDelay = crashDelay;
    this.expectedHearbeatInterval = heartbeatInterval;
  }

  /**
   * Start running the server. The server will start listening for client connections and make a
   * new client handler to deal with each connection. It will also start a heartbeat listener that
   * checks on whether the processes it connects to are alive.
   */
  public void start() {
    Queue<String> reqLog = new ConcurrentLinkedQueue<>();

    // initialize server socket
    ServerSocket serverSocket;
    try {
      serverSocket = new ServerSocket(port);
    } catch (IOException e) {
      throw new RuntimeException("Server error: " + e.getMessage());
    }

    // start listening for heartbeat messages
    HeartbeatListener heartbeatListener = new HeartbeatListener(this.hostname, this.peerId,
            this.port, expectedHearbeatInterval, this.membership, this.peerOrder, this.leaderId);
    heartbeatListener.start();

    while (true) {
      // accept a connection
      Socket clientSocket;
      try {
        clientSocket = serverSocket.accept();
      } catch (IOException e) {
        throw new RuntimeException("Server accept error: " + e.getMessage());
      }

      // create a new thread to handle the connection
      Thread clientHandler = new Thread(new ClientHandler(this.serverClientCommQueue, this.peerId,
              clientSocket, this.membership, this.peerOrder, this.leaderId, this.crashDelay,
              reqLog));
      clientHandler.start();
    }
  }
}
