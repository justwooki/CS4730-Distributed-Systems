package main.java;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * A proposer is a type of process that will propose some value to the other processes that are
 * its acceptors.
 */
public class Proposer extends Process {
  private char value;
  private final int delay;
  private final int proposerId;
  private final List<ProcessInfo> acceptors;
  private final int totalProcesses;

  /**
   * Constructs a new Proposer object. Each proposer has a proposer ID (separate from its process
   * ID), a list of all its acceptors, and the total number of processes in the system. These
   * details will be extracted from the given hostsfile. Each proposer is also its own acceptor,
   * thus it will start its own acceptor process running in a separate thread in this constructor.
   *
   * @param id the unique ID of the process
   * @param name the hostname of the process
   * @param hostsfile the path to the file containing information on all processes
   * @param value the value to propose
   * @param delay the time to delay the proposer before it starts proposing its value
   */
  public Proposer(int id, String name, String hostsfile, char value, int delay) {
    super(id, name);
    this.value = value;
    this.delay = delay;
    this.proposerId = extractProposerId(hostsfile);
    this.acceptors = extractAcceptors(hostsfile);
    this.totalProcesses = extractTotalProcesses(hostsfile);

    // start acceptor side of proposer
    new Thread(() -> new Acceptor(id, name, hostsfile).start()).start();
  }

  /**
   * Extracts the proposer ID of the proposer from the given hostsfile.
   *
   * @param hostsfile the path to the file containing information on all processes
   * @return the proposer ID
   * @throws IllegalArgumentException if the hostsfile cannot be read for some reason
   */
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

  /**
   * Extracts the acceptors of the proposer from the given hostsfile.
   *
   * @param hostsfile if the hostsfile cannot be read for some reason
   * @return the list of acceptors
   * @throws IllegalArgumentException if the hostsfile cannot be read for some reason
   */
  private List<ProcessInfo> extractAcceptors(String hostsfile) throws IllegalArgumentException {
    String targetRole = "acceptor" + this.proposerId;
    List<ProcessInfo> acceptors = new ArrayList<>();
    acceptors.add(this.info);

    try {
      acceptors.addAll(Files.readAllLines(Paths.get(hostsfile)).stream()
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
              }).toList());
    } catch (IOException e) {
      throw new IllegalArgumentException("Proposer error: Issue with reading hostsfile: " +
              e.getMessage());
    }

    return acceptors;
  }

  /**
   * Extracts the total number of processes in the system from the given hostsfile.
   *
   * @param hostsfile if the hostsfile cannot be read for some reason
   * @return the total number of processes in the system
   * @throws IllegalArgumentException if the hostsfile cannot be read for some reason
   */
  private int extractTotalProcesses(String hostsfile) throws IllegalArgumentException {
    try (Stream<String> lines = Files.lines(Paths.get(hostsfile))) {
      return (int) lines.filter(line -> !line.trim().isEmpty()).count();
    } catch (IOException e) {
      throw new IllegalArgumentException("Process error: Issue with reading hostsfile: " +
              e.getMessage());
    }
  }

  @Override
  public void start() {
    // delay
    try {
      Thread.sleep((1 + this.delay) * 1000L);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Proposer error: Thread interrupted");
    }

    // initiate proposal number
    double proposalNum = Double.parseDouble("1." + this.info.getId());

    // broadcast prepare
    handlePrepare(proposalNum);


    // broadcast accept
  }

  /**
   * Handle the broadcasting and acknowledgement of the Prepare message.
   *
   * @param proposalNum the proposal number
   */
  private void handlePrepare(double proposalNum) {
    String msg = Util.prepareMsg(this.info.getId(), "sent", "prepare",
            "n/a", proposalNum);
    BlockingQueue<String> msgRec = new LinkedBlockingQueue<>();
    AtomicInteger ackCount = new AtomicInteger(0);

    // execute broadcasting and acknowledgment collection concurrently
    new Thread(() -> broadcastPrepare(msg, msgRec)).start();
    new Thread(() -> waitForPrepareAck(proposalNum, ackCount, msgRec)).start();

    // the method concludes when the proposer has received a majority of acknowledgements
    while (true) {
      if (ackCount.get() >= (this.totalProcesses / 2) + 1) {
        break;
      }
    }
  }

  /**
   * Broadcasts the Prepare message to all acceptors. After the message is sent, a response will
   * be expected from each acceptor. This response is queued to be handled by a different method
   * that will wait for acknowledgements.
   *
   * @param msg the message to broadcast
   * @param messagesReceived the queue holding the responses
   * @throws RuntimeException if there are any issues with connecting to any acceptor, sending the
   *                          message, or receiving it
   */
  private void broadcastPrepare(String msg, BlockingQueue<String> messagesReceived)
          throws RuntimeException {
    // Broadcast message to acceptors
    for (ProcessInfo acceptor : this.acceptors) {
      new Thread(() -> {
        try (
                Socket socket = new Socket(acceptor.getName(), Util.PORT);
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                DataInputStream in = new DataInputStream(socket.getInputStream())
        ) {
          // send message
          System.err.println(msg);
          out.writeUTF(msg);

          // queue received message
          messagesReceived.put(in.readUTF());
        } catch (IOException | InterruptedException e) {
          throw new RuntimeException("Proposer error: " + e.getMessage());
        }
      }).start();
    }
  }

  /**
   * Waits for a Prepare Acknowledgement from each acceptor after sending a Prepare broadcast.
   *
   * @param proposalNum the proposal number
   * @param ackCount the number of acknowledgements received so far
   * @param messagesReceived the queue holding the responses from the acceptors
   * @throws RuntimeException if there are any issues retrieving the response or the response
   *                          received is invalid
   */
  private void waitForPrepareAck(double proposalNum, AtomicInteger ackCount,
                                 BlockingQueue<String> messagesReceived) throws RuntimeException {
    while (ackCount.get() < this.acceptors.size()) {
      // get received message
      String[] msgRec;
      try {
        msgRec = Util.unpackMsg(messagesReceived.take());
      } catch (InterruptedException e) {
        throw new RuntimeException("Proposer error: " + e.getMessage());
      }

      int senderId = Integer.parseInt(msgRec[0]);
      String messageType = msgRec[2];
      char acceptedValue = Util.strToChar(msgRec[3]);
      double acceptedProposal = Double.parseDouble(msgRec[4]);

      if (!messageType.equals("prepare_ack")) {
        throw new RuntimeException("Proposer error: Received invalid prepare acknowledgment");
      }

      // if any accepted values returned, replace value with it for highest accepted proposal
      if (acceptedValue != '\u0000' && acceptedProposal > proposalNum) {
        this.value = acceptedValue;
      }

      // print received message
      System.err.println(Util.prepareMsg(senderId, "received", messageType,
              Util.charToStr(acceptedValue), acceptedProposal));

      // updated number of acknowledgements received
      ackCount.getAndIncrement();
    }
  }
}
