package Model;

import javafx.stage.Stage;

public final class StageView {
    private static Stage currentstage;

    public StageView(Stage stage) {
        StageView.currentstage = stage;
    }

    public static Stage getStage() {
        return currentstage;
    }

    public void setStage(Stage stage) {
        StageView.currentstage = stage;
    }

    public static void hide() {
        currentstage.hide();
    }

    public static void show() {
        currentstage.show();
    }
}
