package mapprotos;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArrayOfNodeListsTest {

  public static final int NUM_LISTS = 1;
  public static final int NODES_PER_LIST = 7;
  static final int NUM_KEYS = NUM_LISTS * NODES_PER_LIST;

  Integer[] keys = new Integer[NUM_KEYS];

  @BeforeEach
  void beforeEach() {
    Random rnd = new Random(17); // TODO have random keys
    for (int i = 0; i < NUM_KEYS; i++) {
      final int key = i*100;
      keys[i] = key;
    }
  }

  @Test
  void get_arrayOfArraysTest() {
    ArrayOfNodeLists nodeLists =  new ArrayOfArraysOfPointer(NUM_LISTS, NODES_PER_LIST);
    initMap(nodeLists);

    for (int l = 0; l < NUM_LISTS; l++) {
      for (int n = 0; n < NODES_PER_LIST; n++) {
        final int keyIndex = l * NODES_PER_LIST + n;
        final Integer key = keys[keyIndex];
        nodeLists.addNode(l, n, key, key, key);
        System.out.println(keyIndex);
        assertEquals(keys[keyIndex], nodeLists.get(l, key, key));
      }
    }
  }

  private void initMap(ArrayOfNodeLists arrayOfNodeLists) {
    for (int l = 0; l < ArrayOfNodeListsTest.NUM_LISTS; l++) {
      for (int n = ArrayOfNodeListsTest.NODES_PER_LIST -1; n >= 0; n--) {
        final Integer key = keys[l * ArrayOfNodeListsTest.NODES_PER_LIST + n];
        arrayOfNodeLists.addNode(l, n, key, key, key);
      }
    }
  }
}
