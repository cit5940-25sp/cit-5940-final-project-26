package othello.gamelogic;

/**
 * Represents a computer player that will make decisions autonomously during their turns.
 * Employs a specific computer strategy passed in through program arguments.
 */
public class ComputerPlayer extends Player{
    private AIStrategy theComputerStrategy;

    public ComputerPlayer(String strategyName) {
        // PART 2
        // TODO: Use the strategyName input to create a specific strategy class for this computer
        // This input should match the ones specified in App.java!

        theComputerStrategy = null; // Placeholder code for now
    }

    // PART 2
    // TODO: implement a method that returns a BoardSpace that a strategy selects

    /**
     * The actual move will be delegated by this method to the specific AI strategy
     * @param board the Othello game board
     * @return BoardSpace by AI
     */
    public BoardSpace computerMove(BoardSpace[][] board) {
        // If the actual AI strategy is not instantiated, return null
        if (theComputerStrategy == null) {
            return null;
        }

        // Delegation
        return theComputerStrategy.computerMove(board, this);
    }
}