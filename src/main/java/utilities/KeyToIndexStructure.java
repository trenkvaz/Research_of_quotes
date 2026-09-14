package utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeyToIndexStructure<K, V> {
    public final List<V> list = new ArrayList<>();
    public final Map<K, Integer> keyToIndexMap = new HashMap<>();

    // Добавление элемента
    public void add(K key, V value) {
        keyToIndexMap.put(key, list.size());
        list.add(value);
    }

    public V get(K key){
        Integer startIndex = keyToIndexMap.get(key);
        if (startIndex == null) {
            System.out.println("Ключ не найден");
            return null;
        }
        return list.get(startIndex);
    }

    // Поиск и перебор вправо
    public void iterateFromKey(K startKey) {
        Integer startIndex = keyToIndexMap.get(startKey);
        if (startIndex == null) {
            System.out.println("Ключ не найден");
            return;
        }

        // Итерируемся вправо от найденного элемента как по массиву
        for (int i = startIndex; i < list.size(); i++) {
            V element = list.get(i);
            System.out.println("Индекс " + i + ": " + element);
        }
    }

    public int getIndex(K key) { return keyToIndexMap.getOrDefault(key, -1); }
    public V getByEntry(int index) { return list.get(index); }
    public List<V> getFullList() { return list; }
}
