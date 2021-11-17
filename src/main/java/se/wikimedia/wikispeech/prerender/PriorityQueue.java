package se.wikimedia.wikispeech.prerender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PriorityQueue<T> {
    private int maximumSize;

    private List<T> list;
    private Comparator<T> comparator;

    public PriorityQueue(int maximumSize, Comparator<T> comparator) {
        this.maximumSize = maximumSize;
        this.comparator = comparator;
        this.list = new ArrayList<>(maximumSize);
    }

    /**
     * @param item
     * @return Item removed, if applicable
     */
    public T add(T item) {
        // todo binary search find index
        int index = Collections.binarySearch(list, item, comparator);
        if (index < 0) {
            index *= -1;
            index--;
        }
        if (index == maximumSize) {
            return item;
        }
        list.add(index, item);
        if (list.size() > maximumSize) {
            return list.remove(maximumSize);
        }
        return null;
    }

    public T poll() {
        return list.remove(0);
    }

    public T peek() {
        return list.get(0);
    }

    public List<T> toList() {
        return new ArrayList<>(list);
    }
}
