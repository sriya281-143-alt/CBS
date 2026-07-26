module org.personal.campusbookingsystem {

    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires javafx.graphics;

    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;

    opens org.personal.campusbookingsystem to javafx.fxml;
    opens org.personal.campusbookingsystem.controller to javafx.fxml;

    exports org.personal.campusbookingsystem;
    exports org.personal.campusbookingsystem.controller;
}