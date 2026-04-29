package br.com.aluguelequipamento.controller;

import java.io.IOException;

import br.com.App;
import javafx.fxml.FXML;

public class PrimaryController {

    @FXML
    private void switchToSecondary() throws IOException {
        App.setRoot("secondary");
    }
}
