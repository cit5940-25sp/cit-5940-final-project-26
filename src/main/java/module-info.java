module othello {
    requires javafx.controls;
    requires javafx.fxml;
    requires deeplearning4j.nn;
    requires nd4j.api;


    opens othello to javafx.fxml;
    exports othello;
    exports othello.gui;
    opens othello.gui to javafx.fxml;
    exports othello.gamelogic;
    opens othello.gamelogic to javafx.fxml;
}