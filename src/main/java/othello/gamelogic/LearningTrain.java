package othello.gamelogic;


import java.util.List;
import java.util.Map;
import java.io.File;
import java.io.IOException;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;



public class LearningTrain {

    private TDLearningStrategy trained;

    /**
     * Play a single game between two computer players
     * @param playerBlackDisc the player that will place black discs in the game
     * @param playerWhiteDisc the player that will place white discs in the game
     * @return the average error for learning of a single game play
     */
    private double singleGame(ComputerPlayer playerBlackDisc, ComputerPlayer playerWhiteDisc) {
        OthelloGame othelloGame = new OthelloGame(playerBlackDisc, playerWhiteDisc);
        // At first, set game over to false
        boolean gameOver = false;
        Player thisPlayer = othelloGame.getPlayerOne();
        double totalError = 0.0; // Initialize total learning error to 0
        int count = 0;
        int skipsInARow = 0;

        while (!gameOver) { // Loop until game is over
            // Obtain the available moves
            Map<BoardSpace, List<BoardSpace>> availableMoves = othelloGame.getAvailableMoves(thisPlayer);
            if (availableMoves == null || availableMoves.isEmpty()) {
                Player otherPlayer = (thisPlayer == othelloGame.getPlayerOne()) ? othelloGame.getPlayerTwo() :
                        othelloGame.getPlayerOne();
                Map<BoardSpace, List<BoardSpace>> otherPlayerMoves = othelloGame.getAvailableMoves(otherPlayer);
                if (otherPlayerMoves == null || otherPlayerMoves.isEmpty()) {
                    gameOver = true;
                } else {
                    skipsInARow++;
                    // Check if there are two total consecutive skips
                    if (skipsInARow == 2) {
                        gameOver = true;
                    } else {
                        thisPlayer = otherPlayer;
                    }
                }

                continue;
            }

            skipsInARow = 0; // Reaching here resets the consecutive skips
            BoardSpace moveChosen = null;
            // Call two-argument computerMove
            if (thisPlayer instanceof ComputerPlayer) {
                moveChosen = ((ComputerPlayer) thisPlayer).computerMove(othelloGame.getBoard(), thisPlayer);
            }

            if (moveChosen != null) {
                othelloGame.getBoard()[moveChosen.getX()][moveChosen.getY()].setType(thisPlayer.getColor());
                thisPlayer.getPlayerOwnedSpacesSpaces().add(othelloGame.getBoard()[moveChosen.getX()]
                        [moveChosen.getY()]);

                // Obtain a copy of the board
                BoardSpace[][] priorToConversion = getCopy(othelloGame.getBoard());
                // Then call takeSpaces
                othelloGame.takeSpaces(thisPlayer,
                        (thisPlayer == othelloGame.getPlayerOne()) ?
                                othelloGame.getPlayerTwo() : othelloGame.getPlayerOne(),
                        availableMoves, moveChosen);

                if (((ComputerPlayer) thisPlayer).getComputerStrategy() instanceof TDLearningStrategy) {
                    TDLearningStrategy theTDStrategy = (TDLearningStrategy)
                            ((ComputerPlayer) thisPlayer).getComputerStrategy();

                    // Obtain progression error by calling updateInternal
                    double progressError = theTDStrategy.updateInternal(priorToConversion, thisPlayer,
                            0.0, othelloGame.getBoard());
                    totalError += progressError; // Add to total error
                    count++;
                }
            }

            thisPlayer = (thisPlayer == othelloGame.getPlayerOne()) ?
                    othelloGame.getPlayerTwo() : othelloGame.getPlayerOne();

            int allPositionsTaken = playerBlackDisc.getPlayerOwnedSpacesSpaces().size() +
                    playerWhiteDisc.getPlayerOwnedSpacesSpaces().size();
            // Check if the board is full
            if (allPositionsTaken == 64) {
                gameOver = true;
            }
        }

        // Get summary of total discs for each player
        int countBlackDisc = playerBlackDisc.getPlayerOwnedSpacesSpaces().size();
        int countWhiteDisc = playerWhiteDisc.getPlayerOwnedSpacesSpaces().size();
        // Initialize scores for this game
        double playerBlackDiscScore = 0.0;
        double playerWhiteDiscScore = 0.0;
        // Assign score based on the result of the game
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

        // Return the average error
        return (count > 0) ? (totalError / count) : 0.0;
    }

    /**
     * Repeat the training for the custom ML strategy over many games between two custom strategy players
     * @param iterations the number of times the game will be repeated
     */
    public void repeatGame(int iterations) {
        double totalError = 0.0; // Initialize total error to 0
        // Have two custom strategy players play against each other for training
        for (int i = 1; i <= iterations; i++) {
            ComputerPlayer playerBlackDisc = new ComputerPlayer("custom");
            playerBlackDisc.setColor(BoardSpace.SpaceType.BLACK);
            ComputerPlayer playerWhiteDisc = new ComputerPlayer("custom");
            playerWhiteDisc.setColor(BoardSpace.SpaceType.WHITE);

            // Call singleGame to play one game and get error
            double actualError = singleGame(playerBlackDisc, playerWhiteDisc);
            // Add to total error
            totalError += actualError;
            System.out.println("Game iteration: " + i + " average learning error: " + actualError);

            // Choose a player's strategy to assigned to trained
            AIStrategy chosenStrategy = playerBlackDisc.getComputerStrategy();
            if (chosenStrategy instanceof TDLearningStrategy) {
                this.trained = (TDLearningStrategy) chosenStrategy;
            }
        }

        // Compute the ending average error
        double finalAverageError = totalError / iterations;
        System.out.println("Final average error: " + finalAverageError);
    }

    /**
     * Save the trained model
     * @param filePath the destination to save the model
     */
    public void saveTrainedModel(String filePath) throws IOException {
        // The case for no trained model to save
        if (trained == null) {
            System.out.println("There is no model to be saved");
            return;
        }

        // Save the model
        MultiLayerNetwork model = trained.getCNNModel();
        File file = new File(filePath);
        ModelSerializer.writeModel(model, file, true);

        System.out.println("Successfully saved the model to the specified path: " + filePath);
    }

    /**
     * Load a previously trained model
     * @param filePath the source from which to load the model
     * @param gamma Specify the gamma parameter
     * @param alpha Specify the alpha parameter
     */
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

    /**
     * Copy the original board that is passed into the method
     * @param originalBoard the board that will be copied
     * @return a copied board
     */
    private BoardSpace[][] getCopy(BoardSpace[][] originalBoard) {
        int rows = originalBoard.length;
        int columns = originalBoard[0].length;
        BoardSpace[][] copiedBoard = new BoardSpace[rows][columns];

        // Loop over the original board
        for (int x = 0; x < rows; x++) {
            for (int y = 0; y < columns; y++) {
                BoardSpace.SpaceType previousType = originalBoard[x][y].getType();
                copiedBoard[x][y] = new BoardSpace(x, y, previousType);
            }
        }

        return copiedBoard;
    }

    /**
     * The custom ML strategy plays against the minimax strategy. Win rate is evaluated
     * @param iterations the number of times game will be played
     */
    public void trainVsMinimax(int iterations) {
        if (this.trained == null) {
            System.out.println("There is no trained model to play against minimax");
            return;
        }

        // Initialize the ML strategy's wins to 0
        int customWins = 0;
        // Have the trained ML strategy play against the minimax strategy
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

    /**
     * Start the training and evaluating process
     */
    public static void main(String[] args) {
        LearningTrain training = new LearningTrain();

        // First train the naive model by playing custom vs. custom games
        System.out.println("Training with custom vs custom");
        training.repeatGame(50);

        try {
            training.saveTrainedModel(
                    "src/main/java/othello/gamelogic/trainedModel.zip");
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        try {
            training.loadTrainedModel(
                    "src/main/java/othello/gamelogic/trainedModel.zip", 0.90, 0.005);
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        // Then have the trained model play against minimax
        System.out.println("Playing with minimax");
        training.trainVsMinimax(100);
    }
}
