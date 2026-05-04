module br.com {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.postgresql.jdbc;

    opens br.com to javafx.fxml;
    opens br.com.aluguelequipamento.controller to javafx.fxml;

    opens br.com.aluguelequipamento.model.domain to javafx.base;

    exports br.com;
    exports br.com.aluguelequipamento.controller;
}