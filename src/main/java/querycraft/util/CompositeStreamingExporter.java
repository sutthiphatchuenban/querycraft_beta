package querycraft.util;

import querycraft.model.ColumnInfo;
import querycraft.service.StreamingQueryService.StreamingExporter;

import java.util.ArrayList;
import java.util.List;

/**
 * Executes multiple StreamingExporters concurrently for the same data stream.
 */
public class CompositeStreamingExporter implements StreamingExporter {

    private final List<StreamingExporter> exporters = new ArrayList<>();

    public void addExporter(StreamingExporter exporter) {
        if (exporter != null) {
            exporters.add(exporter);
        }
    }

    @Override
    public void start(List<ColumnInfo> columns) throws Exception {
        for (StreamingExporter exporter : exporters) {
            exporter.start(columns);
        }
    }

    @Override
    public void writeRow(Object[] row) throws Exception {
        for (StreamingExporter exporter : exporters) {
            exporter.writeRow(row);
        }
    }

    @Override
    public void finish() throws Exception {
        Exception lastException = null;
        for (StreamingExporter exporter : exporters) {
            try {
                exporter.finish();
            } catch (Exception e) {
                lastException = e;
            }
        }
        if (lastException != null) throw lastException;
    }

    @Override
    public void abort() {
        for (StreamingExporter exporter : exporters) {
            exporter.abort();
        }
    }
}
