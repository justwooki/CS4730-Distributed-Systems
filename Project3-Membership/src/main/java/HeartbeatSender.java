import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Handles periodic broadcasting of heartbeat messages to all peers on the local network.
 */
public class HeartbeatSender {
  private final int port;
  private final int hearbeatInterval;
  private final ScheduledExecutorService scheduler;

  /**
   * Constructor for HeartbeatSender class.
   *
   * @param port the port to broacast the heartbeat message on
   * @param hearbeatInterval the interval (in seconds) between each heartbeat
   */
  public HeartbeatSender(int port, int hearbeatInterval) {
    this.port = port;
    this.hearbeatInterval = hearbeatInterval;
    this.scheduler = Executors.newScheduledThreadPool(1);
  }

  /**
   * Starts the heartbeat sender.
   */
  public void start() {
    this.scheduler.scheduleAtFixedRate(this::sendHeartbeat, 0, this.hearbeatInterval,
            TimeUnit.SECONDS);
  }

  /**
   * Sends a heartbeat message to all peers on the local network.
   */
  private void sendHeartbeat() {
    try (DatagramSocket socket = new DatagramSocket()) {
      socket.setBroadcast(true);

      String message = "HEARTBEAT";
      byte[] buffer = message.getBytes();

      InetAddress address = InetAddress.getByName("255.255.255.255");
      DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, this.port);

      socket.send(packet);
    } catch (Exception e) {
      throw new RuntimeException("HeartbeatSender error: " + e.getMessage());
    }
  }
}
