package mapprotos;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArrayBinHashMapTest {

  static final int NUM_KEYS = 10;

  ArrayBinHashMap<Integer,Integer> map = new ArrayBinHashMap<>(NUM_KEYS);
  int[] keys = new int[NUM_KEYS];

  @BeforeEach
  void beforeEach() {
    Random rnd = new Random(17);
    for (int i = 0; i < NUM_KEYS; i++) {
      final int key = i*100;
      keys[i] = key;
      map.put(key, i);
    }
  }

  @Test
  void getTest() {
    for (int i = 0; i < NUM_KEYS; i++) {
      assertEquals(i, map.get(keys[i]));
    }
  }
}
