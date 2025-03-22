package main.java;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Main class to run the project.
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
//    System.out.println(extractAcceptors("hostsfile-testcase2.txt").stream().map(ProcessInfo::getId));
  }

//  private static List<ProcessInfo> extractAcceptors(String hostsfile) throws IllegalArgumentException {
//    String targetRole = "acceptor5";
//
//    try {
//      return Files.readAllLines(Paths.get(hostsfile)).stream().filter(line -> {
//        String[] parts = line.split(":");
//        if (parts.length < 2) {
//          return false;
//        }
//        return Arrays.asList(parts[1].split(",")).contains(targetRole);
//      }).map(line -> {
//        String peerName = line.split(":")[0];
//        int peerId = Character.getNumericValue(peerName.charAt(peerName.length() - 1));
//        return new ProcessInfo(peerId, peerName);
//      }).toList();
//    } catch (IOException e) {
//      throw new IllegalArgumentException("Proposer error: Issue with reading hostsfile: " +
//              e.getMessage());
//    }

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
            throw new IllegalArgumentException("Missing hostsfile argument");
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

    if (hostsfile == null) {
      throw new IllegalArgumentException("Missing hostsfile argument");
    }

    String name;
    try {
      name = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      throw new RuntimeException("Process error: Unable to determine hostname: " +
              e.getMessage());
    }
    int id = Character.getNumericValue(name.charAt(name.length() - 1));

    switch (getRole(hostsfile, name)) {
      case "proposer" -> {
        if (value == '\u0000') {
          throw new IllegalArgumentException("Missing value argument");
        }
        return new Proposer(id, name, hostsfile, value, delay);
      }
      case "acceptor" -> {
        return new Acceptor(id, name, hostsfile);
      }
    }

    return null;
  }

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
