package main.java;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class Acceptor extends Process {
  private final List<ProcessInfo> proposers;

  public Acceptor(int id, String name, String hostsfile) {
    super(id, name);
    this.proposers = extractProposers(hostsfile);
  }

  private List<ProcessInfo> extractProposers(String hostsfile) throws IllegalArgumentException {
    try {
      List<String> lines = Files.readAllLines(Paths.get(hostsfile));

      return Arrays.stream(lines.stream()
                      .filter(line -> line.startsWith(info.getName())).toList().get(0)
                      .split(":")[1].split(","))
              .map(role -> {
                int proposerId = Character.getNumericValue(role.charAt(role.length() - 1));
                String proposerName = lines.stream()
                        .map(line -> line.split(":"))
                        .filter(line -> line[1].contains("proposer" + proposerId))
                        .toList().get(0)[0];
                int proposerProcessId =
                        Character.getNumericValue(proposerName.charAt(proposerName.length() - 1));
                return new ProcessInfo(proposerProcessId, proposerName);
              }).toList();
    } catch (IOException e) {
      throw new IllegalArgumentException("Acceptor error: Issue with reading hostsfile: " +
              e.getMessage());
    }
  }

  @Override
  public void start() {
    System.out.println(this.info.getName() + " is on at acceptor for proposers " + this.proposers.stream().map(ProcessInfo::getName).toList());
  }
}
