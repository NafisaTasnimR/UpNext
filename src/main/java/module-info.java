module org.example.upnext {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires com.oracle.database.jdbc;


    opens org.example.upnext to javafx.fxml;
    exports org.example.upnext;
}