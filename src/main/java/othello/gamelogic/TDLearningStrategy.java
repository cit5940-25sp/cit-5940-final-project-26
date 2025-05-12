package othello.gamelogic;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.activations.Activation;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.nn.conf.layers.SubsamplingLayer;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.distribution.NormalDistribution;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;

import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.optimize.api.IterationListener;
import org.deeplearning4j.nn.conf.distribution.UniformDistribution;
import org.nd4j.linalg.lossfunctions.LossFunctions;


public class TDLearningStrategy implements AIStrategy {

    private final double gamma;
    private final double alpha;
    private final MultiLayerNetwork CNN;

    public TDLearningStrategy(double gamma, double alpha) {
        this.gamma = gamma;
        this.alpha = alpha;
        this.CNN = theCNNModel();
    }

    public TDLearningStrategy(MultiLayerNetwork model, double gamma, double alpha) {
        this.gamma = gamma;
        this.alpha = alpha;
        this.CNN = model;
    }

    private MultiLayerNetwork theCNNModel() {
        int length = 8;
        int width = 8;
        int encoding = 1;

        MultiLayerConfiguration configuration = new NeuralNetConfiguration.Builder()
                .seed(27).weightInit(WeightInit.XAVIER)
                .updater(new Adam(alpha)).list()
                .layer(0, new ConvolutionLayer.Builder(3, 3)
                        .nIn(encoding)
                        .stride(1, 1)
                        .nOut(16)
                        .activation(Activation.RELU)
                        .build())
                .layer(1, new SubsamplingLayer.Builder()
                        .kernelSize(2, 2)
                        .stride(2, 2)
                        .build())
                .layer(2, new ConvolutionLayer.Builder(3, 3)
                        .nOut(32)
                        .stride(1, 1)
                        .activation(Activation.RELU)
                        .build())
                .layer(3, new DenseLayer.Builder()
                        .nOut(64)
                        .activation(Activation.RELU)
                        .build())
                .layer(4, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .activation(Activation.IDENTITY)
                        .nOut(1)
                        .build())
                .setInputType(InputType.convolutional(length, width, encoding)).build();

        MultiLayerNetwork network = new MultiLayerNetwork(configuration);
        network.init();

        return network;
    }

    /**
     *
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

        double epsilon = 0.05;
        if (Math.random() < epsilon) {
            int chosenIndex = (int) (Math.random() * availableMoves.size());
            return new ArrayList<>(availableMoves.keySet()).get(chosenIndex);
        }

        BoardSpace optimalPosition = null;
        double optimalValue = Double.NEGATIVE_INFINITY;

        for (BoardSpace moveChoice : availableMoves.keySet()) {
            BoardSpace[][] boardCopy = makeBoardCopy(board); // Make a copy of the original board
            move(boardCopy, actingPlayer, moveChoice, availableMoves.get(moveChoice));

            double predictedOutput = predictBoard(boardCopy, actingPlayer);
            if (predictedOutput > optimalValue) {
                optimalValue = predictedOutput;
                optimalPosition = moveChoice;
            }
        }

        return optimalPosition;
    }

    private BoardSpace[][] makeBoardCopy(BoardSpace[][] originalBoard) {
        int rows = originalBoard.length;
        int columns = originalBoard[0].length;
        BoardSpace[][] boardCopy = new BoardSpace[rows][columns];

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                BoardSpace.SpaceType eachCopy = originalBoard[row][column].getType();
                boardCopy[row][column] = new BoardSpace(row, column, eachCopy);
            }
        }

        return boardCopy;
    }

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
     *
     * @param board the standard game board
     * @param actingPlayer current player that is being evaluated
     * @return a single value that shows the player how valuable the board space is
     */
    private double predictBoard(BoardSpace[][] board, Player actingPlayer) {
        double[][] boardInputs = new double[board.length][board[0].length];
        BoardSpace.SpaceType opponent = (actingPlayer.getColor() == BoardSpace.SpaceType.BLACK)
                ? BoardSpace.SpaceType.WHITE : BoardSpace.SpaceType.BLACK;
        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board[r].length; c++) {
                BoardSpace.SpaceType thisType = board[r][c].getType();
                if (thisType == actingPlayer.getColor()) {
                    boardInputs[r][c] = 1.0;
                } else if (thisType == opponent) {
                    boardInputs[r][c] = -1.0;
                } else {
                    boardInputs[r][c] = 0.0;
                }
            }
        }

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
    protected double updateInternal(BoardSpace[][] thisBoard, Player actingPlayer,
                                  double reward, BoardSpace[][] nextBoard) {
        double currentInternalValue = predictBoard(thisBoard, actingPlayer);
        double nextInternalValue = 0.0;
        if (nextBoard != null) {
            nextInternalValue = predictBoard(nextBoard, actingPlayer);
        }

        double learningTarget = reward + gamma * nextInternalValue;
        double learningError = learningTarget - currentInternalValue;

        int rows = thisBoard.length;
        int columns = thisBoard[0].length;
        double[][] boardInputs = new double[rows][columns];
        BoardSpace.SpaceType opponent = (actingPlayer.getColor() == BoardSpace.SpaceType.BLACK)
                ? BoardSpace.SpaceType.WHITE : BoardSpace.SpaceType.BLACK;

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

        INDArray inputs = Nd4j.create(1, 1, rows, columns);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                inputs.putScalar(new int[]{0, 0, r, c}, boardInputs[r][c]);
            }
        }

        INDArray labels = Nd4j.create(1, 1);
        labels.putScalar(0, learningTarget);
        CNN.fit(inputs, labels);

        return Math.abs(learningError);
    }

    public MultiLayerNetwork getCNNModel() {
        return CNN;
    }
}
