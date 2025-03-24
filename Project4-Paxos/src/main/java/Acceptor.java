package main.java;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * An acceptor is a type of process that will accept a value that is proposed to them under certain
 * conditions following the Paxos algorithm.
 */
public class Acceptor extends Process {
  private double minProposal;
  private double acceptedProposal;
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
    this.minProposal = 0.0;
    this.acceptedProposal = 0.0;
    this.acceptedValue = '\u0000';
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

    return switch (messageType) {
      case "prepare" -> handlePrepare(proposalNum);
      case "accept" -> handleAccept(senderId, proposalNum, value);
      default -> throw new RuntimeException("Acceptor error: Unknown message type received");
    };
  }

  /**
   * Handles a Prepare message received by a proposer and prepares a response.
   *
   * @param proposalNum the proposal number received from a proposer
   * @return the response to the proposer
   */
  private String handlePrepare(double proposalNum) {
    System.out.println("our value: " + this.acceptedValue);
    if (proposalNum > this.minProposal) {
      this.minProposal = proposalNum;
    }

    String msg = Util.prepareMsg(this.info.getId(), "sent", "prepare_ack",
            Util.charToStr(this.acceptedValue), this.acceptedProposal);
    System.err.println(msg);

    return msg;
  }

  /**
   * Handles an Accept message received by a proposer and prepares a response.
   *
   * @param senderId the ID of the proposer sending the Accept message
   * @param proposalNum the proposal number received from a proposer
   * @return the response to the proposer
   */
  private String handleAccept(int senderId, double proposalNum, char value) {
    if (proposalNum >= this.minProposal) {
      this.minProposal = proposalNum;
      this.acceptedProposal = proposalNum;
      this.acceptedValue = value;

      // print chosen value
      System.err.println(Util.prepareMsg(senderId, "chose", "chose",
              Util.charToStr(this.acceptedValue), this.acceptedProposal));
    }

    String msg = Util.prepareMsg(this.info.getId(), "sent", "accept_ack",
            Util.charToStr(this.acceptedValue), this.acceptedProposal);
    System.err.println(msg);

    return msg;
  }
}
