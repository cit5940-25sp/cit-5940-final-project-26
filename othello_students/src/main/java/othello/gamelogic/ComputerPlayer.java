package othello.gamelogic;

/**
 * Represents a computer player that will make decisions autonomously during their turns.
 * Employs a specific computer strategy passed in through program arguments.
 */
public class ComputerPlayer extends Player{
    private String strategy;
    private AIStrategy theComputerStrategy;

    public ComputerPlayer(String strategyName) {
        // PART 2
        // TODO: Use the strategyName input to create a specific strategy class for this computer
        // This input should match the ones specified in App.java!
        this.strategy = strategyName;
        theComputerStrategy = null;
    }

    // PART 2
    // TODO: implement a method that returns a BoardSpace that a strategy selects
    public BoardSpace selectedStrategy(BoardSpace[][] board, Player self, Player op, int maxDepth) {
        if (this.strategy.equals("minimax")) {
            Minimax minimax = new Minimax();
            Node root = new Node();
            minimax.buildTree(board, self, op,0, root, maxDepth);
            BoardSpace next = minimax.minimaxStrategyWithMaxDepth(root);
            return next;
        }
        return null;
    }

    public BoardSpace computerMove(BoardSpace[][] board) {
        // If the actual AI strategy is not instantiated, return null
        if (theComputerStrategy == null) {
            return null;
        }

        // Delegation
        return theComputerStrategy.computerMove(board, this);
    }
}