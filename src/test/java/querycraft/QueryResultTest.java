package querycraft;

import static org.junit.Assert.*;
import org.junit.Test;
import querycraft.model.QueryResult;

public class QueryResultTest {

    @Test
    public void testQueryResultInitialization() {
        QueryResult result = new QueryResult();
        assertTrue(result.isSelectQuery());
        assertFalse(result.isTruncated());
        assertNotNull(result.getColumns());
        assertNotNull(result.getRows());
        assertEquals(0, result.getRowCount());
    }

    @Test
    public void testAddRows() {
        QueryResult result = new QueryResult();
        Object[] row1 = {"data1", 123};
        Object[] row2 = {"data2", 456};
        
        result.addRow(row1);
        result.addRow(row2);
        
        assertEquals(2, result.getRowCount());
        assertEquals("data1", result.getRows().get(0)[0]);
        assertEquals(456, result.getRows().get(1)[1]);
    }

    @Test
    public void testTruncationFlag() {
        QueryResult result = new QueryResult();
        assertFalse(result.isTruncated());
        
        result.setTruncated(true);
        assertTrue(result.isTruncated());
        
        result.setTruncated(false);
        assertFalse(result.isTruncated());
    }

    @Test
    public void testGetValueAt() {
        QueryResult result = new QueryResult();
        result.addRow(new Object[]{"A", "B"});
        result.addRow(new Object[]{"C", "D"});
        
        assertEquals("A", result.getValueAt(0, 0));
        assertEquals("D", result.getValueAt(1, 1));
        assertNull(result.getValueAt(2, 0)); // Out of bounds
        assertNull(result.getValueAt(0, 2)); // Out of bounds
    }

    @Test
    public void testErrorHandling() {
        QueryResult result = new QueryResult();
        assertFalse(result.hasError());
        
        result.setErrorMessage("Test Error");
        assertTrue(result.hasError());
        assertEquals("Test Error", result.getErrorMessage());
    }
}
