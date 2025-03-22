import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Process class to store the information of each process. To communicate with other processes, it
 * will start separate threads to split the process into server and client entities.
 */
public class Process {
  private final int startDelay;
  private final int crashDelay;
  private final boolean leaderCrash;
  private final String[] peerOrder; // keeps track of the order of the peers
  private final int port;

  /**
   * Constructs a Process.
   *
   * @param hostsfile the path to the file that contains the list of hostnames that the processes
   *                  are running on; the hostnames are expected to be separated by newlines and
   *                  listed in the order of their process IDs
   * @param startDelay a sleep that should occur at the start of the program - sleeps for "delay"
   *                   seconds before starting any aspects of the protocol
   * @param crashDelay a sleep that should start immediately after sending an JOIN message; when
   *                   the sleep ends, the peer should crash (exit)
   * @param leaderCrash should only be passed to the starting leader, which waits for all hosts in
   *                    hostfile to join, sends a REQ message (of type DEL) to all hosts except the
   *                    next leader, and crashes (exit)
   * @param port the port on which to establish a connection on
   * @throws IllegalArgumentException if the hostsfile cannot be read for some reason
   */
  public Process(String hostsfile, int startDelay, int crashDelay, boolean leaderCrash, int port)
          throws IllegalArgumentException {
    this.startDelay = startDelay;
    this.crashDelay = crashDelay;
    this.leaderCrash = leaderCrash;
    this.port = port;

    // read hostsfile to set peerOrder
    try {
      this.peerOrder = Files.readAllLines(Paths.get(hostsfile)).toArray(new String[0]);
    } catch (IOException e) {
      throw new IllegalArgumentException("Process Error: Error reading hostsfile");
    }
  }

  /**
   * Start running the process. The process will split its operations between a server side to
   * handle incoming connections and a client side to connect with other processes.
   */
  public void start() {
    // set variables
    String hostname;
    try {
      hostname = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      throw new RuntimeException("Process error: Unable to determine hostname: "
              + e.getMessage());
    }

    int peerId = Util.getPeerId(this.peerOrder, hostname);
    Membership membership = new Membership();
    AtomicInteger leaderId = new AtomicInteger(1); // assume leader is first peer
    BlockingQueue<String> serverClientCommQueue = new LinkedBlockingQueue<>();
    int heartbeatInterval = 1;

    // sleep for delayed time before starting the process
    Util.sleep(this.startDelay * 1000);

    // build server and client threads
    Thread serverThread = new Thread(() -> new Server(serverClientCommQueue, hostname, peerId,
            membership, this.port, this.peerOrder, leaderId,
            this.crashDelay, heartbeatInterval).start());
    Thread clientThread = new Thread(() -> new Client(serverClientCommQueue, peerId, membership,
            this.port, this.peerOrder, leaderId, heartbeatInterval).start());

    // run threads
    serverThread.start();
    clientThread.start();
  }
}
