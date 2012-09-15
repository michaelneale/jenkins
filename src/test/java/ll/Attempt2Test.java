package ll;

import ll.Attempt2.Direction;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * @author Kohsuke Kawaguchi
 */
public class Attempt2Test extends Assert {
    // A=1, B=3, C=5
    @Rule
    public FakeMapBuilder aBuilder = new FakeMapBuilder();
    private FakeMap a;

    // empty map
    @Rule
    public FakeMapBuilder bBuilder = new FakeMapBuilder();
    private FakeMap b;

    @Rule
    public FakeMapBuilder localBuilder = new FakeMapBuilder();

    @Before
    public void setUp() throws Exception {
        a = aBuilder.add(1, "A").add(3, "B").add(5, "C").make();

        b = bBuilder.make();
    }

    @Test
    public void lookup() {
        a.get(1).asserts(1,"A");
        assertNull(a.get(2));
        a.get(3).asserts(3,"B");
        assertNull(a.get(4));
        a.get(5).asserts(5,"C");

        assertNull(b.get(1));
        assertNull(b.get(3));
        assertNull(b.get(5));
    }

    @Test
    public void idempotentLookup() {
        for (int i=0; i<5; i++) {
            a.get(1).asserts(1,"A");
            a.get((Object)1).asserts(1, "A");
        }
    }

    @Test
    public void lookupWithBogusKeyType() {
        assertNull(a.get(null));
        assertNull(a.get("foo"));
        assertNull(a.get(this));
    }

    @Test
    public void firstKey() {
        assertEquals(1, a.firstKey().intValue());

        try {
            b.firstKey();
            fail();
        } catch (NoSuchElementException e) {
            // as expected
        }
    }

    @Test
    public void lastKey() {
        assertEquals(5, a.lastKey().intValue());
        try {
            b.lastKey();
            fail();
        } catch (NoSuchElementException e) {
            // as expected
        }
    }

    @Test
    public void search() {
        // searching toward non-existent direction
        assertNull(a.search( 99, Direction.ASC));
        assertNull(a.search(-99, Direction.DESC));
    }

    /**
     * If load fails, search needs to gracefully handle it
     */
    @Test
    public void unloadableData() throws IOException {
        FakeMap m = localBuilder.add(1, "A").addUnloadable("B").add(5, "C").make();

        assertNull(m.search(3,Direction.EXACT));
        m.search(3,Direction.DESC).asserts(1,"A");
        m.search(3,Direction.ASC ).asserts(5,"C");
    }

    @Test
    public void eagerLoading() throws IOException {
        Map.Entry[] b = a.entrySet().toArray(new Map.Entry[3]);
        ((Build)b[0].getValue()).asserts(1,"A");
        ((Build)b[1].getValue()).asserts(3,"B");
        ((Build)b[2].getValue()).asserts(5,"C");
    }

    @Test
    public void fastLookup() throws IOException {
        FakeMap a = localBuilder.addBoth(1, "A").addBoth(3, "B").addBoth(5, "C"). make();

        a.get(1).asserts(1,"A");
        assertNull(a.get(2));
        a.get(3).asserts(3,"B");
        assertNull(a.get(4));
        a.get(5).asserts(5,"C");
    }

    @Test
    public void fastSearch() throws IOException {
        FakeMap a = localBuilder.addBoth(1, "A").addBoth(3, "B").addBoth(5, "C").addBoth(7,"D").make();

        // we should be using the cache to find the entry efficiently
        a.search(6, Direction.ASC).asserts(7,"D");
        a.search(2, Direction.DESC).asserts(1, "A");
    }

    @Test
    public void bogusCache() throws IOException {
        FakeMap a = localBuilder.addUnloadableCache(1).make();
        assertNull(a.get(1));
    }

    @Test
    public void bogusCacheAndHiddenRealData() throws IOException {
        FakeMap a = localBuilder.addUnloadableCache(1).add(1,"A").make();
        a.get(1).asserts(1, "A");
    }

    @Test
    public void bogusCache2() throws IOException {
        FakeMap a = localBuilder.addBogusCache(1,3,"A").make();
        assertNull(a.get(1));
        a.get(3).asserts(3,"A");
    }

    @Test
    public void incompleteCache() throws IOException {
        FakeMapBuilder setup = localBuilder.addBoth(1, "A").add(3, "B").addBoth(5, "C");

        // each test uses a fresh map since cache lookup causes additional loads
        // to verify the results

        // if we just rely on cache,
        // it'll pick up 5:C as the first ascending value,
        // but we should be then verifying this by loading B, so in the end we should
        // find the correct value
        setup.make().search(2, Direction.ASC).asserts(3,"B");
        setup.make().search(4, Direction.DESC).asserts(3,"B");

        // variation of the cache based search where we find the outer-most value via cache
        setup.make().search(0, Direction.ASC).asserts(1,"A");
        setup.make().search(6, Direction.DESC).asserts(5,"C");

        // variation of the cache search where the cache tells us that we are searching
        // in the direction that doesn't have any records
        assertNull(setup.make().search(0, Direction.DESC));
        assertNull(setup.make().search(6, Direction.ASC));
    }
}
