import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Process class to store the information of each process.
 */
public class Process {
  private final String hostname;
  private final int peerId;
  private final int startDelay;
  private final int crashDelay;
  private final boolean leaderCrash;
  private final String[] peerOrder; // keeps track of the order of the peers
  private final Membership membership;
  private final AtomicInteger leaderId;
  private final int port;

  /**
   * Constructor for Process class.
   *
   * @param hostsfile the path to the file that contains the list of hostnames that the processes
   *                  are running on; the hostnames are expected to be separated by newlines and
   *                  listed in the order of their process IDs
   * @param startDelay a sleep that should occur at the start of the program - sleeps for "delay"
   *                   seconds before starting any aspects of the protocol
   * @param crashDelay  a sleep that should start immediately after sending an JOIN message; when
   *                    the sleep ends, the peer should crash (exit)
   * @param leaderCrash should only be passed to the starting leader, which waits for all hosts in
   *                    hostfile to join, sends a REQ message (of type DEL) to all hosts except the
   *                    next leader, and crashes (exit)
   * @throws IllegalArgumentException if the hostsfile cannot be read for some reason
   */
  public Process(String hostsfile, int startDelay, int crashDelay, boolean leaderCrash, int port)
          throws IllegalArgumentException {
    // set hostname
    try {
      this.hostname = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      throw new RuntimeException("Process error: Unable to determine hostname: "
              + e.getMessage());
    }

    this.startDelay = startDelay;
    this.crashDelay = crashDelay;
    this.leaderCrash = leaderCrash;

    // read hostsfile to set peerOrder
    try {
      this.peerOrder = Files.readAllLines(Paths.get(hostsfile)).toArray(new String[0]);
    } catch (IOException e) {
      throw new IllegalArgumentException("Process Error: Error reading hostsfile");
    }

    this.membership = new Membership();
    this.leaderId = new AtomicInteger(1); // leader is always assumed to be the first peer
    this.port = port;

    // set peerId
    int peerId = -1;
    for (int i = 0; i < this.peerOrder.length; i++) {
      if (this.peerOrder[i].equals(this.hostname)) {
        peerId = i + 1;
        break;
      }
    }
    if (peerId == -1) {
      throw new IllegalArgumentException("Process error: Hostname not found in hostsfile");
    }
    this.peerId = peerId;
  }

  public String getHostname() {
    return this.hostname;
  }

  public int getStartDelay() {
    return this.startDelay;
  }

  public int getCrashDelay() {
    return this.crashDelay;
  }

  public boolean isLeaderCrash() {
    return this.leaderCrash;
  }

  public int getViewId() {
    return this.membership.getViewId();
  }

  public String[] getPeerOrder() {
    return this.peerOrder;
  }

  public List<Integer> getPeers() {
    return this.membership.getPeers();
  }

  public int getLeaderId() {
    return this.leaderId.get();
  }

  public void start() {
    Util.sleep(this.startDelay * 1000);

    BlockingQueue<String> queue = new LinkedBlockingQueue<>();

    Thread serverThread = new Thread(() -> new Server(queue, this.hostname, this.peerId,
            this.membership, this.port, this.peerOrder, this.leaderId, this.crashDelay).start());
    Thread clientThread = new Thread(() -> new Client(queue, this.hostname, this.peerId,
            this.membership, this.port, this.peerOrder, this.leaderId).start());

    serverThread.start();
    clientThread.start();
  }
}
