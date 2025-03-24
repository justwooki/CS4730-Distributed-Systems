package main.java;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Main class to run the Paxos program.
 */
public class Main {
  /**
   * Main method to run the project.
   *
   * @param args command line arguments
   * @throws IllegalArgumentException if the command line arguments are invalid
   */
  public static void main(String[] args) throws IllegalArgumentException {
    Process process = constructProcess(args);
    if (process != null) {
      process.start();
    }
  }

  /**
   * Construct and return a new process based on the given arguments.
   *
   * @param args the given arguments that hold the properties of the process
   * @return a new process
   * @throws IllegalArgumentException if the hostsfile cannot be read for some reason
   */
  private static Process constructProcess(String[] args) throws IllegalArgumentException {
    String hostsfile = null;
    char value = '\u0000';
    int delay = 0;

    // Parse command line arguments
    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "-h" -> {
          if (i + 1 < args.length) {
            hostsfile = args[++i];
          } else {
            throw new IllegalArgumentException("Main error: Missing hostsfile argument");
          }
        }
        case "-v" -> {
          if (i + 1 < args.length) {
            value = args[++i].charAt(0);
          } else {
            throw new IllegalArgumentException("Main error: Missing value argument");
          }
        }
        case "-t" -> {
          if (i + 1 < args.length) {
            delay = Integer.parseInt(args[++i]);
          } else {
            throw new IllegalArgumentException("Main error: Missing delay argument");
          }
        }
        default -> throw new IllegalArgumentException("Main error: Invalid argument");
      }
    }

    // hostsname can't be null
    if (hostsfile == null) {
      throw new IllegalArgumentException("Main error: Missing hostsfile argument");
    }

    // get hostname and id of process
    String name;
    try {
      name = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      throw new RuntimeException("Main error: Unable to determine hostname: " +
              e.getMessage());
    }
    int id = Character.getNumericValue(name.charAt(name.length() - 1));

    // get the role of process (proposer/acceptor) and return the process
    switch (getRole(hostsfile, name)) {
      case "proposer" -> {
        if (value == '\u0000') {
          throw new IllegalArgumentException("Main error: Missing value argument");
        }
        return new Proposer(id, name, hostsfile, value, delay);
      }
      case "acceptor" -> {
        return new Acceptor(id, name, hostsfile);
      }
    }

    return null;
  }

  /**
   * Gets the role of the process from the given hostsfile based on the given hostname of the
   * process. Process roles are divided into Proposer and Acceptor.
   *
   * @param hostsfile the path to the file containing information on the process
   * @param name the hostname of the process
   * @return the role of the process that is found
   * @throws IllegalArgumentException if the hostsfile cannot be read for some reason
   */
  private static String getRole(String hostsfile, String name) throws IllegalArgumentException {
    try {
      return Files.readAllLines(Paths.get(hostsfile)).stream()
              .filter(str -> str.startsWith(name)).toList().get(0)
              .split(":")[1]
              .split(",")[0]
              .replaceAll("\\d", "");
    } catch (IOException e) {
      throw new IllegalArgumentException("Main error: Issue with reading hostsfile: " +
              e.getMessage());
    }
  }
}
