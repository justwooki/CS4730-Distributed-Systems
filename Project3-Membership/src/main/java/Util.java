import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

/**
 * Utility class to make life easier.
 */
class Util {
  protected static final int BUFFER_SIZE = 1024;

  /**
   * Get the hostname of a socket.
   *
   * @param socket the socket
   * @return the hostname
   */
  protected static String getHostname(Socket socket) {
    return socket.getInetAddress().getHostName().split("\\.")[0];
  }

  /**
   * Convert a list of integers to a comma-separated string. Brackets are included.
   *
   * @param list list of integers
   * @return comma-separated string
   */
  protected static String listToString(List<Integer> list) {
    return list.stream().map(String::valueOf)
            .collect(Collectors.joining(",", "[", "]"));
  }

  /**
   * Convert a comma-separated string to a list of integers. The string must be formatted as the
   * result of a toString call on a list of integers.
   *
   * @param str comma-separated string
   * @return list of integers
   */
  protected static List<Integer> stringToList(String str) {
    return Arrays.stream(str.substring(1, str.length() - 1).split(", "))
            .map(Integer::parseInt).toList();
  }

  /**
   * Get the peer id of a given hostname.
   *
   * @param arr the array of hostnames
   * @param peerHostname the hostname to get the id of
   * @return the peer id
   * @throws IllegalArgumentException if the peer id cannot be found
   */
  protected static int getPeerId(String[] arr, String peerHostname)
          throws IllegalArgumentException {
    for (int i = 0; i < arr.length; i++) {
      if (arr[i].equals(peerHostname)) {
        return i + 1;
      }
    }
    throw new IllegalArgumentException("Peer ID cannot be found");
  }

  /**
   * Queues a message to the thread-safe queue.
   *
   * @param queue the queue to send the message to
   * @param msg the message to send
   */
  protected static void queueMessage(BlockingQueue<String> queue, String msg) {
    try {
      queue.put(msg);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Client handler error: Thread interrupted");
    }
  }

  /**
   * Dequeues a message from the thread-safe queue.
   *
   * @param queue the queue to dequeue the message from
   * @return the message
   */
  protected static String dequeueMessage(BlockingQueue<String> queue) {
    try {
      return queue.take();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Client handler error: Thread interrupted");
    }
  }

  /**
   * Sleep for a given number of milliseconds.
   *
   * @param ms the number of milliseconds to sleep
   */
  protected static void sleep(int ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Client handler error: Thread interrupted");
    }
  }
}
