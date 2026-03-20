module querycraft {
    requires javafx.controls;
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

    /* 
     * NOTE: JDBC Drivers and some internal dependencies are 
     * loaded dynamically at runtime to avoid IDE warnings 
     * about unstable automatic module names.
     */

    // Open packages for reflection by JavaFX
    opens querycraft to javafx.graphics, javafx.fxml;
    opens querycraft.ui to javafx.fxml;
    opens querycraft.ui.component to javafx.fxml;

    // Export packages for visibility
    exports querycraft;
    exports querycraft.model;
    exports querycraft.service;
    exports querycraft.ui;
    exports querycraft.ui.component;
    exports querycraft.util;
}
