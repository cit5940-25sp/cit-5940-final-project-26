package othello.gamelogic;


import java.util.List;
import java.util.Map;
import java.io.File;
import java.io.IOException;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;



public class LearningTrain {

    private TDLearningStrategy trained;

    private double singleGame(ComputerPlayer playerBlackDisc, ComputerPlayer playerWhiteDisc) {
        OthelloGame othelloGame = new OthelloGame(playerBlackDisc, playerWhiteDisc);
        boolean gameOver = false;
        Player thisPlayer = othelloGame.getPlayerOne();
        double totalError = 0.0;
        int count = 0;
        int skipsInARow = 0;

        while (!gameOver) {
            //System.out.println("This is for player: " + thisPlayer.getColor());
            Map<BoardSpace, List<BoardSpace>> availableMoves = othelloGame.getAvailableMoves(thisPlayer);
            if (availableMoves == null || availableMoves.isEmpty()) {
                Player otherPlayer = (thisPlayer == othelloGame.getPlayerOne()) ? othelloGame.getPlayerTwo() :
                        othelloGame.getPlayerOne();
                Map<BoardSpace, List<BoardSpace>> otherPlayerMoves = othelloGame.getAvailableMoves(otherPlayer);
                if (otherPlayerMoves == null || otherPlayerMoves.isEmpty()) {
                    gameOver = true;
                } else {
                    skipsInARow++;
                    if (skipsInARow == 2) {
                        gameOver = true;
                    } else {
                        thisPlayer = otherPlayer;
                    }
                }

                continue;
            }

            skipsInARow = 0;
            BoardSpace moveChosen = null;
            if (thisPlayer instanceof ComputerPlayer) {
                moveChosen = ((ComputerPlayer) thisPlayer).computerMove(othelloGame.getBoard(), thisPlayer);
            }

            if (moveChosen != null) {
                othelloGame.getBoard()[moveChosen.getX()][moveChosen.getY()].setType(thisPlayer.getColor());
                thisPlayer.getPlayerOwnedSpacesSpaces().add(othelloGame.getBoard()[moveChosen.getX()]
                        [moveChosen.getY()]);

                BoardSpace[][] priorToConversion = getCopy(othelloGame.getBoard());

                othelloGame.takeSpaces(thisPlayer,
                        (thisPlayer == othelloGame.getPlayerOne()) ?
                                othelloGame.getPlayerTwo() : othelloGame.getPlayerOne(),
                        availableMoves, moveChosen);

                if (((ComputerPlayer) thisPlayer).getComputerStrategy() instanceof TDLearningStrategy) {
                    TDLearningStrategy theTDStrategy = (TDLearningStrategy)
                            ((ComputerPlayer) thisPlayer).getComputerStrategy();

                    double progressError = theTDStrategy.updateInternal(priorToConversion, thisPlayer,
                            0.0, othelloGame.getBoard());
                    totalError += progressError;
                    count++;
                }
            }

            thisPlayer = (thisPlayer == othelloGame.getPlayerOne()) ?
                    othelloGame.getPlayerTwo() : othelloGame.getPlayerOne();

            int allPositionsTaken = playerBlackDisc.getPlayerOwnedSpacesSpaces().size() +
                    playerWhiteDisc.getPlayerOwnedSpacesSpaces().size();
            if (allPositionsTaken == 64) {
                gameOver = true;
            }

            //System.out.println("Positions taken:" + allPositionsTaken);
        }

        int countBlackDisc = playerBlackDisc.getPlayerOwnedSpacesSpaces().size();
        int countWhiteDisc = playerWhiteDisc.getPlayerOwnedSpacesSpaces().size();
        double playerBlackDiscScore = 0.0;
        double playerWhiteDiscScore = 0.0;
        if (countBlackDisc > countWhiteDisc) {
            playerBlackDiscScore = 1.0;
            playerWhiteDiscScore = -1.0;
        } else if (countBlackDisc < countWhiteDisc) {
            playerBlackDiscScore = -1.0;
            playerWhiteDiscScore = 1.0;
        } else {
            playerBlackDiscScore = 0.0;
            playerWhiteDiscScore = 0.0;
        }

        if (playerBlackDisc instanceof ComputerPlayer) {
            AIStrategy playerBlackDiscStrategy = ((ComputerPlayer) playerBlackDisc).getComputerStrategy();
            if (playerBlackDiscStrategy instanceof TDLearningStrategy) {
                double playerBlackDiscErr = ((TDLearningStrategy) playerBlackDiscStrategy).updateInternal(
                        othelloGame.getBoard(), playerBlackDisc,
                        playerBlackDiscScore, null);
                totalError += playerBlackDiscErr;
                count++;
            }
        }

        if (playerWhiteDisc instanceof ComputerPlayer) {
            AIStrategy playerWhiteDiscStrategy = ((ComputerPlayer) playerWhiteDisc).getComputerStrategy();
            if (playerWhiteDiscStrategy instanceof TDLearningStrategy) {
                double playerWhiteDiscErr = ((TDLearningStrategy) playerWhiteDiscStrategy).updateInternal(
                        othelloGame.getBoard(), playerWhiteDisc,
                        playerWhiteDiscScore, null);
                totalError += playerWhiteDiscErr;
                count++;
            }
        }

        return (count > 0) ? (totalError / count) : 0.0;
    }

    public void repeatGame(int iterations) {
        double totalError = 0.0;
        for (int i = 1; i <= iterations; i++) {
            ComputerPlayer playerBlackDisc = new ComputerPlayer("custom");
            playerBlackDisc.setColor(BoardSpace.SpaceType.BLACK);
            ComputerPlayer playerWhiteDisc = new ComputerPlayer("custom");
            playerWhiteDisc.setColor(BoardSpace.SpaceType.WHITE);

            double actualError = singleGame(playerBlackDisc, playerWhiteDisc);
            totalError += actualError;
            System.out.println("Game iteration: " + i + " average learning error: " + actualError);

            AIStrategy chosenStrategy = playerBlackDisc.getComputerStrategy();
            if (chosenStrategy instanceof TDLearningStrategy) {
                this.trained = (TDLearningStrategy) chosenStrategy;
            }
        }

        double finalAverageError = totalError / iterations;
        System.out.println("Final average error: " + finalAverageError);
    }

    public void saveTrainedModel(String filePath) throws IOException {
        if (trained == null) {
            System.out.println("There is no model to be saved");
            return;
        }

        MultiLayerNetwork model = trained.getCNNModel();
        File file = new File(filePath);
        ModelSerializer.writeModel(model, file, true);

        System.out.println("Successfully saved the model to the specified path: " + filePath);
    }

    public void loadTrainedModel(String filePath, double gamma, double alpha) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("The specified model cannot be found at: " + filePath);
            return;
        }

        MultiLayerNetwork model = ModelSerializer.restoreMultiLayerNetwork(file);
        this.trained = new TDLearningStrategy(model, gamma, alpha);
        System.out.println("Successfully loaded model from the specified path: " + filePath);
    }

    private BoardSpace[][] getCopy(BoardSpace[][] originalBoard) {
        int rows = originalBoard.length;
        int columns = originalBoard[0].length;
        BoardSpace[][] copiedBoard = new BoardSpace[rows][columns];

        for (int x = 0; x < rows; x++) {
            for (int y = 0; y < columns; y++) {
                BoardSpace.SpaceType previousType = originalBoard[x][y].getType();
                copiedBoard[x][y] = new BoardSpace(x, y, previousType);
            }
        }

        return copiedBoard;
    }

    public void trainVsMinimax(int iterations) {
        if (this.trained == null) {
            System.out.println("There is no trained model to play against minimax");
            return;
        }

        int customWins = 0;
        for (int i = 1; i <= iterations; i++) {
            ComputerPlayer blackDiscCustom = new ComputerPlayer("custom");
            blackDiscCustom.setColor(BoardSpace.SpaceType.BLACK);
            blackDiscCustom.setCustomStrategy(this.trained);

            ComputerPlayer whiteDiscMinimax = new ComputerPlayer("minimax");
            whiteDiscMinimax.setColor(BoardSpace.SpaceType.WHITE);

            // Execute a single game
            singleGame(blackDiscCustom, whiteDiscMinimax);

            // Obtain the summary stats
            int customCounts = blackDiscCustom.getPlayerOwnedSpacesSpaces().size();
            int minimaxCounts = whiteDiscMinimax.getPlayerOwnedSpacesSpaces().size();
            // If the custom ML strategy wins, increase its win count by one
            if (customCounts > minimaxCounts) {
                customWins++;
                //System.out.println("The ML strategy wins the game");
            }
        }

        double customWinRate = (double) customWins / iterations;
        System.out.println("The ML strategy wins a total of: " + customWins + " games");
        System.out.println("The ML strategy win rate is: " + (customWinRate * 100.0));
    }

    public void noTrainingCustomVsMinimax(int iterations) {
        int customWins = 0;
        for (int i = 1; i <= iterations; i++) {
            ComputerPlayer blackDiscCustom = new ComputerPlayer("custom");
            blackDiscCustom.setColor(BoardSpace.SpaceType.BLACK);

            ComputerPlayer whiteDiscMinimax = new ComputerPlayer("minimax");
            whiteDiscMinimax.setColor(BoardSpace.SpaceType.WHITE);

            singleGame(blackDiscCustom, whiteDiscMinimax);
            int customCounts = blackDiscCustom.getPlayerOwnedSpacesSpaces().size();
            int minimaxCounts = whiteDiscMinimax.getPlayerOwnedSpacesSpaces().size();
            if (customCounts > minimaxCounts) {
                customWins++;
            }
        }

        double customWinRate = (double) customWins / iterations * 100;
        System.out.println("The untrained ML strategy win rate is: " + customWinRate);
    }

    public static void main(String[] args) {
        LearningTrain training = new LearningTrain();

        // First train the naive model by playing custom vs. custom games
        System.out.println("Training with custom vs custom");
        training.repeatGame(1);

        try {
            training.saveTrainedModel(
                    "src/main/java/othello/gamelogic/trainedModel.zip");
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        try {
            training.loadTrainedModel(
                    "src/main/java/othello/gamelogic/trainedModel.zip", 0.99, 0.01);
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        // Then have the trained model play against minimax
        System.out.println("Training with minimax");
        training.trainVsMinimax(100);
    }
}
