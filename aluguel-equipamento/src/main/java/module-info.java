module br.com {
    requires javafx.controls;
    requires javafx.fxml;

    opens br.com to javafx.fxml;
    exports br.com;
}
