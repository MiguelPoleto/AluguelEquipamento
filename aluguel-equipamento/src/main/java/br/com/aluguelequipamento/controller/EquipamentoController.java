package br.com.aluguelequipamento.controller;

import java.io.IOException;

import br.com.App;
import javafx.fxml.FXML;

public class EquipamentoController {


    @FXML
    private void switchToPrimary() throws IOException {
        App.setRoot("primary");
    }
}