import java.net.UnknownHostException;

public class Main {
  private static final int PORT = 7000;

  public static void main(String[] args) throws UnknownHostException, IllegalArgumentException {
    Process process = readArgs(args);
    process.start();
  }

  /**
   * Read the command line arguments and return a Process object.
   *
   * @param args the command line arguments
   * @return a Process object
   * @throws IllegalArgumentException if the command line arguments are invalid
   * @throws UnknownHostException if the hostname cannot be determined
   */
  private static Process readArgs(String[] args) throws IllegalArgumentException,
          UnknownHostException {
    String hostsfile = null;
    int startDelay = 0;
    int crashDelay = -1; // default value -1 means no crash
    boolean leaderCrash = false;

    // Parse command line arguments
    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "-h" -> {
          if (i + 1 < args.length) {
            hostsfile = args[++i];
          } else {
            throw new IllegalArgumentException("Missing hostsfile argument");
          }
        }
        case "-d" -> {
          if (i + 1 < args.length) {
            startDelay = Integer.parseInt(args[++i]);
          } else {
            throw new IllegalArgumentException("Missing start delay argument");
          }
        }
        case "-c" -> {
          if (i + 1 < args.length) {
            crashDelay = Integer.parseInt(args[++i]);
          } else {
            throw new IllegalArgumentException("Missing crash delay argument");
          }
        }
        case "-t" -> leaderCrash = true;
      }
    }

    if (hostsfile == null) {
      throw new IllegalArgumentException("Missing hostsfile argument");
    }

    return new Process(hostsfile, startDelay, crashDelay, leaderCrash, PORT);
  }
}
