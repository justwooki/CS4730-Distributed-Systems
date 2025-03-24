package main.java;

/**
 * Data class to store a proposal number and character value.
 */
public class ProposalValuePair {
  private final double proposalNum;
  private final char value;

  /**
   * Constructs a new ProposalValuePair object.
   *
   * @param proposalNum the proposal number
   * @param value the character value
   */
  public ProposalValuePair(double proposalNum, char value) {
    this.proposalNum = proposalNum;
    this.value = value;
  }

  /**
   * Gets the proposal number.
   *
   * @return the proposal number
   */
  public double getProposalNum() {
    return this.proposalNum;
  }

  /**
   * Gets the value.
   *
   * @return the value
   */
  public char getValue() {
    return this.value;
  }
}
