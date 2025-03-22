package main.java;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class Proposer extends Process {
  private char value;
  private final int delay;
  private final int proposerId;
  private final List<ProcessInfo> acceptors;

  public Proposer(int id, String name, String hostsfile, char value, int delay) {
    super(id, name);
    this.value = value;
    this.delay = delay;
    this.proposerId = extractProposerId(hostsfile);
    this.acceptors = extractAcceptors(hostsfile);
  }

  private int extractProposerId(String hostsfile) throws IllegalArgumentException {
    try {
      return Integer.parseInt(Files.readAllLines(Paths.get(hostsfile)).stream()
              .filter(line -> line.startsWith(info.getName())).toList().get(0)
              .split(":")[1]
              .replaceAll("\\D", ""));
    } catch (IOException e) {
      throw new IllegalArgumentException("Proposer error: Issue with reading hostsfile: " +
              e.getMessage());
    }
  }

  private List<ProcessInfo> extractAcceptors(String hostsfile) throws IllegalArgumentException {
    String targetRole = "acceptor" + this.proposerId;

    try {
      return Files.readAllLines(Paths.get(hostsfile)).stream()
              .filter(line -> {
                String[] parts = line.split(":");
                if (parts.length < 2) {
                  return false;
                }
                return Arrays.asList(parts[1].split(",")).contains(targetRole);
              }).map(line -> {
                String peerName = line.split(":")[0];
                int peerId = Character.getNumericValue(peerName.charAt(peerName.length() - 1));
                return new ProcessInfo(peerId, peerName);
              }).toList();
    } catch (IOException e) {
      throw new IllegalArgumentException("Proposer error: Issue with reading hostsfile: " +
              e.getMessage());
    }
  }

  @Override
  public void start() {
    // delay
    try {
      Thread.sleep(this.delay * 1000L);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Proposer error: Thread interrupted");
    }

    System.out.println(this.info.getName() + " is on as proposer with acceptors " + this.acceptors.stream().map(ProcessInfo::getName).toList());

    // initiate proposal
    // broadcast prepare
    // broadcast accept
  }
}
