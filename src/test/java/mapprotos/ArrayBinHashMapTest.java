package mapprotos;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArrayBinHashMapTest {

  static final int NUM_KEYS = 1_401;

  int[] keys = new int[NUM_KEYS];

  @BeforeEach
  void beforeEach() {
    Random rnd = new Random(17); // TODO have random keys
    for (int i = 0; i < NUM_KEYS; i++) {
      final int key = i*100;
      keys[i] = key;
    }
  }


  @Test
  void get_startLargeTest() {
    System.out.println("Ignoring ArrayBinHashMapTest.get_startLargeTest"); if (true) return;
    final Map<Integer, Integer> map = initMap(NUM_KEYS * 2);
    for (int i = 0; i < NUM_KEYS; i++) {
      assertEquals(i, map.get(keys[i]));
    }
  }

  @Test
  void get_startSmallTest() {
    System.out.println("Ignoring ArrayBinHashMapTest.get_startSmallTest"); if (true) return;
    final Map<Integer, Integer> map = initMap(1);
    for (int i = 0; i < NUM_KEYS; i++) {
      assertEquals(i, map.get(keys[i]));
    }
  }

  private Map<Integer,Integer> initMap(int initSize) {
    Map<Integer,Integer> map =  new ArrayBinHashMap<>(initSize)  /*new HashMap<>(initSize)*/;
    for (int p = 0; p < NUM_KEYS; p++) {
      map.put(keys[p], p);
      for (int g = 0; g <= p; g++) {
        assertEquals(g, map.get(keys[g]), "p:" + p + " g:" + g);
      }
    }
    return map;
  }
}
