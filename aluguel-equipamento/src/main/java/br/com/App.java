package br.com;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("primary"), 900, 600);
        stage.setTitle("Sistema de Aluguel de Equipamentos");
        stage.setScene(scene);
        stage.show();
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        var resource = App.class.getResource(
                "/br/com/aluguelequipamento/view/" + fxml + ".fxml");
        if (resource == null) {
            throw new IOException("FXML não encontrado: " + fxml + ".fxml");
        }
        FXMLLoader loader = new FXMLLoader(resource);
        return loader.load();
    }

    public static void main(String[] args) {
        launch();
    }
}