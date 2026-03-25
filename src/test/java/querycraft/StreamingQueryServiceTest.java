package querycraft;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import querycraft.exception.QueryCraftException;
import querycraft.model.ColumnInfo;
import querycraft.model.CsvConnectionInfo;
import querycraft.service.DatabaseConnectionService;
import querycraft.service.StreamingQueryService;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class StreamingQueryServiceTest {

    private File tempCsvFolder;
    private StreamingQueryService streamingService;

    @Before
    public void setUp() throws Exception {
        // Create a temporary folder with a CSV file
        tempCsvFolder = new File(System.getProperty("java.io.tmpdir"), "querycraft_test_csv");
        if (!tempCsvFolder.exists()) {
            tempCsvFolder.mkdirs();
        }

        File fakeData = new File(tempCsvFolder, "users.csv");
        try (PrintWriter pw = new PrintWriter(new FileWriter(fakeData))) {
            pw.println("id,name,role");
            for (int i = 1; i <= 50; i++) {
                pw.println(i + ",User" + i + ",Tester");
            }
        }

        // Initialize connection to CSV folder
        CsvConnectionInfo info = new CsvConnectionInfo(tempCsvFolder.getAbsolutePath());
        DatabaseConnectionService.getInstance().connect(info);

        streamingService = new StreamingQueryService();
    }

    @After
    public void tearDown() {
        DatabaseConnectionService.getInstance().disconnect();
        
        // Cleanup temp files
        File[] files = tempCsvFolder.listFiles();
        if (files != null) {
            for (File f : files) f.delete();
        }
        tempCsvFolder.delete();
    }

    @Test
    public void testEstimateRowCount() throws Exception {
        long count = streamingService.estimateRowCount("SELECT * FROM \"users\"");
        assertEquals("Should accurately estimate 50 rows in the CSV", 50, count);
    }

    @Test
    public void testStreamQuery() throws Exception {
        final List<ColumnInfo> parsedColumns = new ArrayList<>();
        final List<Object[]> parsedRows = new ArrayList<>();
        final CountDownLatch latch = new CountDownLatch(1);

        streamingService.streamQuery("SELECT * FROM \"users\" WHERE id <= 10",
                parsedColumns::addAll,
                row -> {
                    synchronized (parsedRows) {
                        parsedRows.add(row);
                    }
                },
                new StreamingQueryService.StreamCallback() {
                    @Override
                    public void onComplete(long totalRows, long durationMs) {
                        latch.countDown();
                    }

                    @Override
                    public void onError(QueryCraftException e) {
                        fail("Streaming failed: " + e.getMessage());
                        latch.countDown();
                    }
                });

        // Wait up to 5 seconds for background thread to finish
        assertTrue(latch.await(5, TimeUnit.SECONDS));

        assertEquals("Should parse exactly 3 columns", 3, parsedColumns.size());
        assertEquals("ID", parsedColumns.get(0).getName().toUpperCase());
        assertEquals("NAME", parsedColumns.get(1).getName().toUpperCase());
        assertEquals("ROLE", parsedColumns.get(2).getName().toUpperCase());

        assertEquals("Should fetch exactly 10 rows due to WHERE clause", 10, parsedRows.size());
        assertEquals("1", parsedRows.get(0)[0].toString()); // DB driver often returns string for CSV
        assertEquals("User1", parsedRows.get(0)[1].toString());
        assertEquals("User10", parsedRows.get(9)[1].toString());
    }
}
