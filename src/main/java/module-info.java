module org.example.upnext {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.upnext to javafx.fxml;
    exports org.example.upnext;
}