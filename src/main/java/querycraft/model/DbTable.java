package querycraft.model;

/**
 * Model class representing a database table or view.
 */
public class DbTable {
    private final String name;
    private final String type;

    public DbTable(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean isView() {
        return "VIEW".equalsIgnoreCase(type) || (type != null && type.contains("VIEW"));
    }

    @Override
    public String toString() {
        if (isView()) {
            return name + " [VIEW]";
        }
        return name;
    }
}
