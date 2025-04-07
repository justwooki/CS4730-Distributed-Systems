package main.java;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility class to make life easier.
 */
public final class Utils {
  public static final int PORT = 7000; // universal port number

  /**
   * Builds a message string from an ordered map of key-value pairs.
   *
   * @param fields a LinkedHashMap containing the key-value pairs to be included in the message in
   *               the desired order
   * @return a formatted message string with the given fields in the order provided
   */
  public static String prepareMsg(LinkedHashMap<String, String> fields) {
    StringBuilder msg = new StringBuilder("{");
    Iterator<Map.Entry<String, String>> iterator = fields.entrySet().iterator();

    while (iterator.hasNext()) {
      Map.Entry<String, String> entry = iterator.next();
      msg.append(entry.getKey()).append(":").append(entry.getValue());
      if (iterator.hasNext()) {
        msg.append(", ");
      }
    }

    msg.append("}");
    return msg.toString();
  }

  /**
   * Unpacks a string message and separates the components into a map of key-value pairs.
   *
   * @param msg the string message
   * @return a map containing the key-value pairs extracted from the message
   */
  public static Map<String, String> unpackMsg(String msg) {
    Map<String, String> unpackedMsg = new HashMap<>();
    String[] parts = msg.substring(1, msg.length() - 1).split(", ");

    for (String part : parts) {
      String[] pair = part.split(":");
      String key = pair[0];
      String value = pair[1];
      unpackedMsg.put(key, value);
    }

    return unpackedMsg;
  }

  /**
   * Extracts numeric portion from the peer ID.
   *
   * @param peerId the peer ID string
   * @return the numeric portion of the peer ID
   */
  public static int extractPeerIdNum(String peerId) {
    return Integer.parseInt(peerId.replaceAll("\\D", ""));
  }
}
