package mapprotos;

public interface ArrayOfNodeLists {
  void addNode(int tableIndex, int binIndex, int intKey, Object objKey, Object value);

  Object get(int intTableIndex, int intKey, Object objKey);
}
