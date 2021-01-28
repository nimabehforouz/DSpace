package org.dspace.statistics.util;

import org.dspace.statistics.util.IPTable.IPFormatException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;

/**
 *
 * @author mwood
 */
public class IPTableTest {

    public IPTableTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of add method, of class IPTable.
     * @throws java.lang.Exception passed through.
     */
    @Ignore
    @Test
    public void testAdd() throws Exception {
    }

    /**
     * Test of contains method, of class IPTable.
     * @throws java.lang.Exception passed through.
     */
    @Test(expected=IPFormatException.class)
    public void testContains() throws Exception {
        String localhost = "127.0.0.1";
        IPTable instance = new IPTable();
        instance.add(localhost);
        boolean contains;

        contains = instance.contains(localhost);
        assertTrue("Address that was add()ed should match", contains);

        contains = instance.contains("192.168.1.1");
        assertFalse("Address that was not add()ed should not match", contains);

        contains = instance.contains("fec0:0:0:1::2");
        assertFalse("IPv6 address should not match anything.", contains);

        // This should throw an IPFormatException.
        contains = instance.contains("axolotl");
        assertFalse("Nonsense string should raise an exception.", contains);
    }

    /**
     * Test of toSet method, of class IPTable.
     */
    @Ignore
    @Test
    public void testToSet() {
    }
}
