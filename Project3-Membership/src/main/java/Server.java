import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Server class to handle incoming connections.
 */
public class Server {
  private final BlockingQueue<String> queue;
  private final String hostname;
  private final int peerId;
  private final ServerSocket serverSocket;
  private final Membership membership;
  private final int port;
  private final String[] peerOrder;
  private final AtomicInteger leaderId;

  public Server(BlockingQueue<String> queue, String hostname, int peerId, Membership membership,
                int port, String[] peerOrder, AtomicInteger leaderId) {
    this.queue = queue;
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
  }

  public void start() {
    while (true) {
      // accept a connection
      Socket clientSocket;
      try {
        clientSocket = this.serverSocket.accept();
      } catch (IOException e) {
        throw new RuntimeException("Server accept error: " + e.getMessage());
      }

      // create a new thread to handle the connection
      Thread clientHandler = new Thread(new ClientHandler(this.queue, this.hostname, this.peerId, clientSocket,
              this.membership, this.port, this.peerOrder, this.leaderId));
      clientHandler.start();
    }
  }
}
