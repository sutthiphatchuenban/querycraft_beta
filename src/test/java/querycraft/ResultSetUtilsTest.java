package querycraft;

import org.junit.Test;
import querycraft.model.ColumnInfo;
import querycraft.util.ResultSetUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ResultSetUtilsTest {

    @Test
    public void testExtractColumnsWithNullMetadata() throws Exception {
        List<ColumnInfo> columns = ResultSetUtils.extractColumns(null);
        assertNotNull(columns);
        assertTrue(columns.isEmpty());
    }

    @Test
    public void testExtractColumnsFromMetadata() throws Exception {
        ResultSetMetaData metaData = createMetaData(new Object[][]{
                {"id", "INTEGER", Types.INTEGER, 11, ResultSetMetaData.columnNoNulls},
                {"name", "VARCHAR", Types.VARCHAR, 255, ResultSetMetaData.columnNullable}
        });

        List<ColumnInfo> columns = ResultSetUtils.extractColumns(metaData);

        assertEquals(2, columns.size());
        assertEquals("id", columns.get(0).getName());
        assertEquals("INTEGER", columns.get(0).getTypeName());
        assertFalse(columns.get(0).isNullable());

        assertEquals("name", columns.get(1).getName());
        assertEquals("VARCHAR", columns.get(1).getTypeName());
        assertTrue(columns.get(1).isNullable());
    }

    @Test
    public void testExtractColumnsWithLimit() throws Exception {
        ResultSetMetaData metaData = createMetaData(new Object[][]{
                {"col1", "VARCHAR", Types.VARCHAR, 10, ResultSetMetaData.columnNullable},
                {"col2", "VARCHAR", Types.VARCHAR, 10, ResultSetMetaData.columnNullable},
                {"col3", "VARCHAR", Types.VARCHAR, 10, ResultSetMetaData.columnNullable}
        });

        List<ColumnInfo> columns = ResultSetUtils.extractColumns(metaData, 2);

        assertEquals(2, columns.size());
        assertEquals("col1", columns.get(0).getName());
        assertEquals("col2", columns.get(1).getName());
    }

    @Test
    public void testExtractColumnNames() {
        List<ColumnInfo> columns = Arrays.asList(
                new ColumnInfo("id", "INTEGER", Types.INTEGER, 11, false),
                new ColumnInfo("name", "VARCHAR", Types.VARCHAR, 255, true)
        );

        String[] names = ResultSetUtils.extractColumnNames(columns);
        assertArrayEquals(new String[]{"id", "name"}, names);
    }

    @Test
    public void testExtractColumnNamesWithNullList() {
        assertArrayEquals(new String[0], ResultSetUtils.extractColumnNames(null));
    }

    private ResultSetMetaData createMetaData(Object[][] columns) {
        Map<Integer, Object[]> columnMap = new HashMap<>();
        for (int i = 0; i < columns.length; i++) {
            columnMap.put(i + 1, columns[i]);
        }

        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                String name = method.getName();
                if ("getColumnCount".equals(name)) {
                    return columns.length;
                }
                if (args != null && args.length == 1 && args[0] instanceof Integer) {
                    Object[] column = columnMap.get((Integer) args[0]);
                    if (column == null) {
                        return defaultValue(method.getReturnType());
                    }
                    return switch (name) {
                        case "getColumnLabel" -> column[0];
                        case "getColumnTypeName" -> column[1];
                        case "getColumnType" -> column[2];
                        case "getColumnDisplaySize" -> column[3];
                        case "isNullable" -> column[4];
                        default -> defaultValue(method.getReturnType());
                    };
                }
                return defaultValue(method.getReturnType());
            }
        };

        return (ResultSetMetaData) Proxy.newProxyInstance(
                ResultSetMetaData.class.getClassLoader(),
                new Class[]{ResultSetMetaData.class},
                handler
        );
    }

    private Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == double.class) {
            return 0.0d;
        }
        if (type == float.class) {
            return 0.0f;
        }
        return null;
    }
}
