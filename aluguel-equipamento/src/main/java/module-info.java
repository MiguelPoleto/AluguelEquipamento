module br.com {

    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;

    requires org.postgresql.jdbc;

    requires jasperreports;

    opens br.com to javafx.fxml;
    opens br.com.aluguelequipamento.controller to javafx.fxml;

    opens br.com.aluguelequipamento.model.domain to javafx.base, jasperreports;

    exports br.com;
    exports br.com.aluguelequipamento.controller;
}
