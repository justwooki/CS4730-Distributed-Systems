package main.java;

/**
 * Utility class to make life easier.
 */
class Util {
  protected static final int PORT = 7000; // universal port number

  /**
   * Prepare a message in a specific format regarding the information passed in.
   *
   * @param senderId the ID of the process who sent the message
   * @param action the action of the message ("sent"/"received"/"chose")
   * @param messageType the type of message ("prepare"/"prepare_ack"/"accept"/"accept_ack"/"chose")
   * @param messageValue the value of the message ("X"/"Y")
   * @param proposalNum the proposal number
   * @return the message in a string format
   */
  protected static String prepareMsg(int senderId, String action, String messageType,
                                     String messageValue, double proposalNum) {
    return "{\"peer_id\":" + senderId + ", \"action\":\"" + action +
            "\", \"message_type\":\"" + messageType + "\", \"message_value\":\"" + messageValue +
            "\", proposal_number\":" + proposalNum + "}";
  }

  /**
   * Unpacks a string message and separates the components into an array of strings.
   *
   * @param msg the string message
   * @return key components of the string message
   */
  protected static String[] unpackMsg(String msg) {
    String[] parts = msg.substring(1, msg.length() - 1).split(", ");
    String[] unpackedMsg = new String[parts.length];

    for (int i = 0; i < parts.length; i++) {
      String value = parts[i].split(":")[1];
      if (value.startsWith("\"")) {
        value = value.substring(1, value.length() - 1);
      }
      unpackedMsg[i] = value;
    }

    return unpackedMsg;
  }

  /**
   * Converts a character type into a string type.
   *
   * @param c the character to convert to a string
   * @return the string version of the character
   */
  protected static String charToStr(char c) {
    return (c == '\u0000') ? "n/a" : c + "";
  }

  /**
   * Converts a string type into a character type.
   *
   * @param s the string to convert to a character
   * @return the character version of the string
   */
  protected static char strToChar(String s) {
    return (s.equals("n/a")) ? '\u0000' : s.charAt(0);
  }
}
