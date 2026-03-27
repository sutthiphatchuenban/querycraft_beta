@SuppressWarnings("module")
module querycraft {
    requires transitive javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;
    requires java.base;
    requires java.desktop;
    requires java.prefs;
    requires transitive java.sql;
    
    // Automatic modules from lib/
    requires org.apache.commons.csv;
    requires org.fxmisc.richtext;
    requires reactfx; // Required for EventStream in SqlEditor
    requires org.fxmisc.undo;
    requires org.fxmisc.flowless;
    requires com.h2database;
    
    // Logging (automatic modules)
    requires org.slf4j;
    
    // Connection Pooling (automatic module)
    requires com.zaxxer.hikari;

    /*
     * NOTE: JDBC Drivers (MySQL, PostgreSQL, SQL Server) are
     * loaded dynamically at runtime to avoid IDE warnings
     * about unstable automatic module names.
     */

    // Open packages for reflection by JavaFX
    opens querycraft to javafx.graphics, javafx.fxml;

    // Export packages for visibility
    exports querycraft;
    exports querycraft.model;
    exports querycraft.connection;
    exports querycraft.query;
    exports querycraft.export;
    exports querycraft.ui.controller;
    exports querycraft.ui.dialog;
    exports querycraft.ui.component;
    exports querycraft.util;
    exports querycraft.exception;
}
