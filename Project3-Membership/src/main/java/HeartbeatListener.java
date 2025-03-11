import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Listens for heartbeat messages from other peers and checks for dead peers.
 */
public class HeartbeatListener {
  private final String hostname;
  private final int peerId;
  private final int port;
  private final int expectedHearbeatInterval;
  private final Membership membership;
  private final String[] peerOrder;
  private final AtomicInteger leaderId;
  private final int timeout;
  private final Map<String, Long> alivePeers;
  private final ExecutorService updaterExecutor;
  private final ExecutorService listenerExecutor;
  private final ScheduledExecutorService scheduler;
  private final List<String> deadPeers; // carries only dead peers not yet removed from membership

  /**
   * Constructor for HeartbeatListener class.
   *
   * @param hostname the hostname of the local machine
   * @param peerId the id of the local peer
   * @param port the port to listen for heartbeat messages on
   * @param expectedHearbeatInterval the expected interval (in seconds) between each heartbeat
   * @param membership the membership object
   * @param peerOrder the order of the peers in the system
   * @param leaderId the id of the leader
   */
  public HeartbeatListener(String hostname, int peerId, int port, int expectedHearbeatInterval,
                           Membership membership, String[] peerOrder, AtomicInteger leaderId) {
    this.hostname = hostname;
    this.peerId = peerId;
    this.port = port;
    this.expectedHearbeatInterval = expectedHearbeatInterval;
    this.membership = membership;
    this.peerOrder = peerOrder;
    this.leaderId = leaderId;
    this.timeout = expectedHearbeatInterval * 2;

    this.alivePeers = new ConcurrentHashMap<>();
    for (int processId : this.membership.getPeers()) {
      this.alivePeers.put(peerOrder[processId - 1], 0L);
    }

    this.updaterExecutor = Executors.newSingleThreadExecutor();
    this.listenerExecutor = Executors.newSingleThreadExecutor();
    this.scheduler = Executors.newScheduledThreadPool(1);
    this.deadPeers = new ArrayList<>();
  }

  /**
   * Starts the heartbeat listener.
   */
  public void start() {
    this.alivePeers.replaceAll((peer, lastTimeSeen) -> System.currentTimeMillis());
    this.updaterExecutor.execute(this::updateAliveAndDeadPeers);
    this.listenerExecutor.execute(this::listenForHeartbeats);
    this.scheduler.scheduleAtFixedRate(this::checkForDeadPeers, 0,
            this.expectedHearbeatInterval, TimeUnit.SECONDS);
  }

  /**
   * Updates list of alive and dead peers.
   */
  private void updateAliveAndDeadPeers() {
    while (true) {
      List<Integer> actualAlivePeers = this.membership.getPeers();
      for (int peerId : actualAlivePeers) {
        String peer = this.peerOrder[peerId - 1];
        if (!this.alivePeers.containsKey(peer)) {
          this.alivePeers.put(peer, System.currentTimeMillis());
        }
      }

      for (String peer : this.alivePeers.keySet()) {
        if (!actualAlivePeers.contains(Util.getPeerId(this.peerOrder, peer))) {
          this.alivePeers.remove(peer);
        }
      }

      this.deadPeers.removeIf(deadPeer -> !this.alivePeers.containsKey(deadPeer));
    }
  }

  /**
   * Listens for heartbeat messages from other peers.
   */
  private void listenForHeartbeats() {
    try (DatagramSocket socket = new DatagramSocket(this.port)) {
      byte buffer[] = new byte[Util.BUFFER_SIZE];

      while (true) {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        String message = new String(packet.getData(), 0, packet.getLength());
        String sender = packet.getAddress().getCanonicalHostName().split("\\.")[0];

        if (message.equals("HEARTBEAT")) {
          this.alivePeers.put(sender, System.currentTimeMillis());
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("HeartbeatListener error: " + e.getMessage());
    }
  }

  /**
   * Checks for dead peers.
   */
  private void checkForDeadPeers() {
    long currentTime = System.currentTimeMillis();
    for (Map.Entry<String, Long> peer : this.alivePeers.entrySet()) {
      // skip yourself and dead peers
      if (peer.getKey().equals(this.hostname) || this.deadPeers.contains(peer.getKey())) {
        continue;
      }

      if (currentTime - peer.getValue() > this.timeout * 1000L) {
        int peerId = Util.getPeerId(this.peerOrder, peer.getKey());

        String message;
        if (peerId == this.leaderId.get()) {
          message = "\"peer " + peerId + " (leader) unreachable\"";
        } else {
          message = "\"peer " + peerId + " unreachable\"";
        }

        System.err.println("{peer_id: " + this.peerId + ", view_id: " +
                this.membership.getViewId() + ", leader: " + this.leaderId.get() +
                ", message:" + message + "}");

        this.deadPeers.add(peer.getKey());

        // if this hearbeat listener is the leader, send a DEL message to the server counterpart
        if (this.peerId == this.leaderId.get()) {
          sendDeadMessage(peerId);
        }
      }
    }
  }

  /**
   * Sends a message to the server counterpart indicating a dead peer.
   *
   * @param deadPeerId the id of the dead peer
   */
  private void sendDeadMessage(int deadPeerId) {
    try (Socket socket = new Socket(this.hostname, this.port)) {
      DataOutputStream out = new DataOutputStream(socket.getOutputStream());
      out.writeUTF("DEADPEER:" + deadPeerId);
      out.close();
    } catch (Exception e) {
      throw new RuntimeException("HeartbeatListener error:" + e.getMessage());
    }
  }
}
