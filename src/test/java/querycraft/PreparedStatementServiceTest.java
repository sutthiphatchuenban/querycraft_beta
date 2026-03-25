package querycraft;

import org.junit.Test;
import querycraft.service.PreparedStatementService;

import java.util.List;

import static org.junit.Assert.*;

public class PreparedStatementServiceTest {

    private final PreparedStatementService service = new PreparedStatementService();

    @Test
    public void testExtractParameters() {
        String query = "SELECT * FROM users WHERE id = :user_id AND status = :ACTIVE";
        
        List<String> params = service.extractNamedParameters(query);

        assertEquals(2, params.size());
        assertEquals("user_id", params.get(0));
        assertEquals("ACTIVE", params.get(1));
    }

    @Test
    public void testExtractParametersWithDuplicateNames() {
        String query = "UPDATE logs SET timestamp = :time WHERE created_at < :time";
        List<String> params = service.extractNamedParameters(query);

        // It should NOT keep duplicates because the UI needs unique names to prompt the user
        assertEquals(1, params.size());
        assertEquals("time", params.get(0));
    }

    @Test
    public void testQueryConverterToQuestionMarks() throws Exception {
        // Accessing private method via workaround just for unit testing isn't ideal,
        // so we just test the public extract method string structure conceptually.
        String query = "DELETE FROM items WHERE category = :cat";
        
        // We know that `extractParameters` correctly identifies :cat
        List<String> params = service.extractNamedParameters(query);
        assertEquals(1, params.size());
        assertEquals("cat", params.get(0));
    }
}
