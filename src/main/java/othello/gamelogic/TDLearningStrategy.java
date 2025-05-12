package othello.gamelogic;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;

import org.deeplearning4j.nn.conf.ConvolutionMode;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.activations.Activation;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.nn.conf.layers.SubsamplingLayer;
import org.deeplearning4j.nn.conf.layers.DenseLayer;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;

import org.deeplearning4j.nn.conf.inputs.InputType;
import org.nd4j.linalg.lossfunctions.LossFunctions;


public class TDLearningStrategy implements AIStrategy {

    private final double gamma;
    private final double alpha;
    private final MultiLayerNetwork CNN;

    /**
     * Two-argument
     * @param gamma the gamma parameter value
     * @param alpha the alpha parameter value
     */
    public TDLearningStrategy(double gamma, double alpha) {
        this.gamma = gamma;
        this.alpha = alpha;
        this.CNN = theCNNModel();
    }

    /**
     * Three-argument
     * @param model the model
     * @param gamma the gamma parameter value
     * @param alpha the alpha parameter value
     */
    public TDLearningStrategy(MultiLayerNetwork model, double gamma, double alpha) {
        this.gamma = gamma;
        this.alpha = alpha;
        this.CNN = model;
    }

    private MultiLayerNetwork theCNNModel() {
        int length = 8;
        int width = 8;
        int channels = 1;

        // Build up the configurations of the model
        MultiLayerConfiguration configuration = new NeuralNetConfiguration.Builder()
                .seed(27)
                .weightInit(WeightInit.XAVIER)
                .updater(new Adam(alpha))
                .convolutionMode(ConvolutionMode.Same)
                .list()
                // Use RELU activationi mostly
                .layer(0, new ConvolutionLayer.Builder(3, 3)
                        .nIn(channels)
                        .stride(1, 1)
                        .nOut(64)
                        .activation(Activation.RELU)
                        .build())
                .layer(1, new ConvolutionLayer.Builder(3, 3)
                        .stride(1, 1)
                        .nOut(64)
                        .activation(Activation.RELU)
                        .build())
                .layer(2, new SubsamplingLayer.Builder()
                        .kernelSize(2, 2)
                        .stride(2, 2)
                        .build())
                .layer(3, new ConvolutionLayer.Builder(3, 3)
                        .stride(1, 1)
                        .nOut(128)
                        .activation(Activation.RELU)
                        .build())
                .layer(4, new ConvolutionLayer.Builder(3, 3)
                        .stride(1, 1)
                        .nOut(128)
                        .activation(Activation.RELU)
                        .build())
                .layer(5, new SubsamplingLayer.Builder()
                        .kernelSize(2, 2)
                        .stride(2, 2)
                        .build())
                .layer(6, new DenseLayer.Builder()
                        .nOut(256)
                        .activation(Activation.RELU)
                        .build())
                .layer(7, new DenseLayer.Builder()
                        .nOut(128)
                        .activation(Activation.RELU)
                        .build())
                // Use linear for the output layer
                .layer(8, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .activation(Activation.IDENTITY)
                        .nOut(1)
                        .build())
                .setInputType(InputType.convolutional(length, width, channels))
                .build();

        MultiLayerNetwork network = new MultiLayerNetwork(configuration);
        network.init();
        return network;
    }

    /**
     * Decide the best move from future game board status
     * @param board the Othello game board
     * @param actingPlayer the computer player that is moving
     * @return the determined board position for the computer player
     */
    @Override
    public BoardSpace computerMove(BoardSpace[][] board, Player actingPlayer) {
        // Obtain all available moves
        Map<BoardSpace, List<BoardSpace>> availableMoves = actingPlayer.getAvailableMoves(board);
        if (availableMoves == null || availableMoves.isEmpty()) {
            return null;
        }

        // Set the exploration threshold
        double epsilon = 0.05;
        if (Math.random() < epsilon) {
            int chosenIndex = (int) (Math.random() * availableMoves.size());
            return new ArrayList<>(availableMoves.keySet()).get(chosenIndex);
        }

        BoardSpace optimalPosition = null;
        double optimalValue = Double.NEGATIVE_INFINITY;

        for (BoardSpace moveChoice : availableMoves.keySet()) {
            // Make a copy of the original board and then call move
            BoardSpace[][] boardCopy = makeBoardCopy(board);
            move(boardCopy, actingPlayer, moveChoice, availableMoves.get(moveChoice));

            double predictedOutput = predictBoard(boardCopy, actingPlayer);
            // The condition for updating optimal position and value
            if (predictedOutput > optimalValue) {
                optimalValue = predictedOutput;
                optimalPosition = moveChoice;
            }
        }

        return optimalPosition;
    }

    /**
     * Make a copy of the board passed into the method
     * @param originalBoard the board that will be copied
     * @return the copied board
     */
    private BoardSpace[][] makeBoardCopy(BoardSpace[][] originalBoard) {
        int rows = originalBoard.length;
        int columns = originalBoard[0].length;
        BoardSpace[][] boardCopy = new BoardSpace[rows][columns];

        // Loop over the board
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                BoardSpace.SpaceType eachCopy = originalBoard[row][column].getType();
                boardCopy[row][column] = new BoardSpace(row, column, eachCopy);
            }
        }

        return boardCopy;
    }

    /**
     * Placing and flipping on a copy board
     * @param boardCopy the copy board
     * @param actingPlayer the specific player moving
     * @param potentials the determined location to place
     * @param startingPoints the origin locations
     */
    private void move(BoardSpace[][] boardCopy, Player actingPlayer, BoardSpace potentials,
                      List<BoardSpace> startingPoints) {
        boardCopy[potentials.getX()][potentials.getY()].setType(actingPlayer.getColor());
        for (BoardSpace startingPoint : startingPoints) {
            int rowChange = Integer.compare(potentials.getX(), startingPoint.getX());
            int columnChange = Integer.compare(potentials.getY(), startingPoint.getY());
            int thisRow = startingPoint.getX();
            int thisColumn = startingPoint.getY();

            while (true) {
                boardCopy[thisRow][thisColumn].setType(actingPlayer.getColor());
                if (thisRow == potentials.getX() && thisColumn == potentials.getY()) {
                    break;
                }

                thisRow += rowChange;
                thisColumn += columnChange;
            }
        }
    }

    /**
     * Run forward CNN and obtain predicted value for board state
     * @param board the standard game board
     * @param actingPlayer current player that is being evaluated
     * @return a single value that shows the player how valuable the board space is
     */
    private double predictBoard(BoardSpace[][] board, Player actingPlayer) {
        double[][] boardInputs = new double[board.length][board[0].length];
        // Obtain the opponent player's color
        BoardSpace.SpaceType opponent = (actingPlayer.getColor() == BoardSpace.SpaceType.BLACK)
                ? BoardSpace.SpaceType.WHITE : BoardSpace.SpaceType.BLACK;
        // Loop over the board
        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board[r].length; c++) {
                BoardSpace.SpaceType thisType = board[r][c].getType();
                if (thisType == actingPlayer.getColor()) {
                    // If location is this player then assign 1
                    boardInputs[r][c] = 1.0;
                } else if (thisType == opponent) {
                    // If location is opponent then assign -1
                    boardInputs[r][c] = -1.0;
                } else {
                    // Empty location is 0
                    boardInputs[r][c] = 0.0;
                }
            }
        }

        // Build a four-dimensional IND array and use the assigned values on the array
        INDArray inputs = Nd4j.create(1, 1, board.length, board[0].length);
        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board[r].length; c++) {
                inputs.putScalar(new int[]{0, 0, r, c}, boardInputs[r][c]);
            }
        }

        INDArray output = CNN.output(inputs);
        return output.getDouble(0);
    }

    /**
     *
     * @param thisBoard the current board status
     * @param actingPlayer the player that is moving
     * @param reward the reward for the outcome -- 1 if wining, 0 if not winning
     * @param nextBoard the next board status
     */
    public double updateInternal(BoardSpace[][] thisBoard, Player actingPlayer,
                                  double reward, BoardSpace[][] nextBoard) {
        // Obtain this board's value
        double currentInternalValue = predictBoard(thisBoard, actingPlayer);
        double nextInternalValue = 0.0;
        if (nextBoard != null) {
            // Obtain the next board's value
            nextInternalValue = predictBoard(nextBoard, actingPlayer);
        }

        // Compute the error by subtracting current value from the target
        double learningTarget = reward + gamma * nextInternalValue;
        double learningError = learningTarget - currentInternalValue;

        int rows = thisBoard.length;
        int columns = thisBoard[0].length;
        double[][] boardInputs = new double[rows][columns];
        BoardSpace.SpaceType opponent = (actingPlayer.getColor() == BoardSpace.SpaceType.BLACK)
                ? BoardSpace.SpaceType.WHITE : BoardSpace.SpaceType.BLACK;

        // Use the numeric values for this board's status
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                BoardSpace.SpaceType thisType = thisBoard[row][column].getType();
                if (thisType == actingPlayer.getColor()) {
                    boardInputs[row][column] = 1.0;
                } else if (thisType == opponent) {
                    boardInputs[row][column] = -1.0;
                } else {
                    boardInputs[row][column] = 0.0;
                }
            }
        }

        // Build 4D array
        INDArray inputs = Nd4j.create(1, 1, rows, columns);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                inputs.putScalar(new int[]{0, 0, r, c}, boardInputs[r][c]);
            }
        }

        // Run fit step for the CNN based on the error
        INDArray labels = Nd4j.create(1, 1);
        labels.putScalar(0, learningTarget);
        CNN.fit(inputs, labels);

        return Math.abs(learningError);
    }

    /**
     *
     * @return the relevant CNN network
     */
    public MultiLayerNetwork getCNNModel() {
        return CNN;
    }
}
