package mapprotos;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Objects;

public class ArrayOfLinkedList implements ArrayOfNodeLists {

  private Node[] table;

  public ArrayOfLinkedList(int tableLen, int binLen) {
    this.table = new Node[tableLen];
  }

  @Override
  public void addNode(int tableIndex, int binIndex, int intKey, Object objKey, Object value) {
    Node newNode = new Node(intKey, objKey, value);
    if (table[tableIndex] == null) {
      // Empty linked list, set the head
      table[tableIndex] = newNode;
    } else {
      // Traverse to the last node and add the new node at the end
      Node current = table[tableIndex];
      while (current.next != null) {
        current = current.next;
      }
      current.next = newNode;
    }
  }

  @Override
  public Object get(int tableIndex, int intKey, Object objKey) {
    Node current = table[tableIndex];
    while (current != null) {
      if (current.intKey == intKey &&
          (current.objKey == objKey || (current.objKey != null && current.objKey.equals(objKey)))) {
        return current.value;
      }
      current = current.next;
    }
    return null;
  }

  public void dumpStats(PrintStream out) {
    SizeInfo sizeInfo = heapSize();
    long bytesPer = sizeInfo.heapSize / sizeInfo.numNodes;
    out.printf("%s instance: numElements: %d%n", this.getClass().getName(), sizeInfo.numNodes);
    out.printf("    heap heapSize: %d(bytes), avg bytes per entry: %d, table len: %d%n",
        sizeInfo.heapSize, bytesPer, table.length);
    long[] types = entryTypes();
    out.printf("    values: %d, empty: %d%n", types[0], types[1]);
  }

  private long[] entryTypes() {
    long[] counts = new long[3];
    for (Object te : table) {
      counts[te!=null ? 1 : 0]++; // TODO changed but not verified
    }
    return counts;
  }

  private SizeInfo heapSize() {
    long acc = objectSizeMaybe(this);
    acc += objectSizeMaybe(table);
    int numNodes = 0;

    Node[] tab = table;
    for (Node currNode : tab) {
      while (currNode != null) {
        acc += objectSizeMaybe(currNode);
        currNode = currNode.next;
        numNodes++;
      }
    }
    return new SizeInfo(numNodes, acc);
  }

  // TODO Factor out these methods into some shared base class before sending this class.
  private static final Method mObjectSize = getObjectSizeMethod();

  private static Method getObjectSizeMethod() {
    try {
      Method m = Objects.class.getDeclaredMethod("getObjectSize", Object.class);
      return m;
    } catch (NoSuchMethodException nsme) {
      return null;
    }
  }

  private long objectSizeMaybe(Object o) {
    try {
      return (mObjectSize != null)
          ? (long)mObjectSize.invoke(null, o)
          : 0L;
    } catch (IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
      return 0L;
    }
  }

  // Implement dumpStats, entryTypes, and heapSize methods if needed (similar to the original class)

  static class SizeInfo {
    int numNodes;
    long heapSize;

    SizeInfo(int numNodes, long heapSize) {
      this.numNodes = numNodes;
      this.heapSize = heapSize;
    }
  }

  static class Node {
    final int intKey;
    final Object objKey;
    final Object value;
    Node next;

    public Node(int intKey, Object objKey, Object value) {
      this.intKey = intKey;
      this.objKey = objKey;
      this.value = value;
      this.next = null;
    }
  }
}
