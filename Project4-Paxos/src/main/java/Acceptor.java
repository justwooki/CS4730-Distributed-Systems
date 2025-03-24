package main.java;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * An acceptor is a type of process that will accept a value that is proposed to them under certain
 * conditions following the Paxos algorithm. Each proposal keeps track of an up-to-date proposal
 * number and an accepted value that gets updated by its proposers.
 */
public class Acceptor extends Process {
  private final List<ProcessInfo> proposers;
  private double minProposal;
  private char acceptedValue;

  /**
   * Constructs a new Acceptor object. Each acceptor has a list of proposers they may accept a
   * value from. This information is found in the given hostsfile.
   *
   * @param id the unique ID of the process
   * @param name the hostname of the process
   * @param hostsfile the path to the file containing information on all processes
   */
  public Acceptor(int id, String name, String hostsfile) {
    super(id, name);
    this.proposers = extractProposers(hostsfile);
    this.minProposal = 0.0;
    this.acceptedValue = '\u0000';
  }

  /**
   * Constructs a new Acceptor object. Each acceptor has a list of proposers they may accept a
   * value from. This information is found in the given hostsfile.
   *
   * @param id the unique ID of the process
   * @param name the hostname of the process
   * @param hostsfile the path to the file containing information on all processes
   * @param acceptedValue the initial accepted value
   */
  protected Acceptor(int id, String name, String hostsfile, char acceptedValue) {
    this(id, name, hostsfile);
    this.acceptedValue = acceptedValue;
    this.minProposal = 0.0;
  }

  /**
   * Extracts the proposers of the acceptors from the given hostsfile.
   *
   * @param hostsfile the path to the file containing information on all processes
   * @return the lists of proposers
   * @throws IllegalArgumentException if the hostsfile cannot be read for some reason
   */
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
    new Thread(() -> {
      try(ServerSocket acceptorSocket = new ServerSocket(Util.PORT)) {
        while (true) {
          Socket proposerSocket = acceptorSocket.accept();
          new Thread(() -> handleConnection(proposerSocket)).start();
        }
      } catch (IOException e) {
        throw new RuntimeException("Acceptor error: " + e.getMessage());
      }
    }).start();
  }

  /**
   * Handle each connection with a proposer.
   *
   * @param proposerSocket the socket of the proposer
   * @throws RuntimeException if there are any connection issues
   */
  private void handleConnection(Socket proposerSocket) throws RuntimeException {
    try (
            DataInputStream in = new DataInputStream(proposerSocket.getInputStream());
            DataOutputStream out = new DataOutputStream(proposerSocket.getOutputStream())
    ) {
      String msg = in.readUTF();
      String response = handleMessage(msg);
      out.writeUTF(response);
    } catch (IOException e) {
      throw new RuntimeException("Acceptor error: " + e.getMessage());
    }
  }

  /**
   * Process a message received by a proposer and prepares a response.
   *
   * @param msg the message received by the proposer
   * @return the response to the proposer
   */
  private String handleMessage(String msg) {
    String[] msgRec = Util.unpackMsg(msg);
    int senderId = Integer.parseInt(msgRec[0]);
    String messageType = msgRec[2];
    char value = Util.strToChar(msgRec[3]);
    double proposalNum = Double.parseDouble(msgRec[4]);
    System.err.println(Util.prepareMsg(senderId, "received", messageType,
            Util.charToStr(value), proposalNum));

    if (messageType.equals("prepare")) {
      return handlePrepare(proposalNum);
    } else {
      return messageType;
    }
  }

  /**
   * Handles a Prepare message received by a proposer and prepares a response.
   *
   * @param proposalNum the proposal number received from a proposer
   * @return the response to the proposer
   */
  private String handlePrepare(double proposalNum) {
    if (proposalNum > minProposal) {
      minProposal = proposalNum;
    }

    String msg = Util.prepareMsg(this.info.getId(), "sent", "prepare_ack",
            Util.charToStr(this.acceptedValue), this.minProposal);
    System.err.println(msg);

    return msg;
  }
}
