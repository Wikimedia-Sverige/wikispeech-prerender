package se.wikimedia.wikispeech.prerender;

import org.junit.Assert;
import org.junit.Test;

import java.util.Comparator;

public class TestPriorityQueue {

    @Test
    public void test() {
        PriorityQueue<Integer> queue = new PriorityQueue<>(5, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o1.compareTo(o2);
            }
        });
        Assert.assertNull(queue.add(5));
        Assert.assertNull(queue.add(3));
        Assert.assertNull(queue.add(6));
        Assert.assertNull(queue.add(1));
        Assert.assertNull(queue.add(0));
        Assert.assertEquals(7, queue.add(7).intValue());
        Assert.assertEquals(9, queue.add(9).intValue());
        Assert.assertEquals(10, queue.add(10).intValue());
        Assert.assertEquals(6, queue.add(2).intValue());
        Assert.assertEquals(5, queue.add(4).intValue());
    }

}
