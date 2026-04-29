module br.com {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
 
    opens br.com to javafx.fxml;
    exports br.com;
}
