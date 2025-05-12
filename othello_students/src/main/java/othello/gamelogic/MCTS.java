package othello.gamelogic;

import othello.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MCTS {

    private int times;

    private Player self;

    private Player op;

    private BoardSpace[][] board;


    public static void main(String[] args) {
        int times = 1000;
        Player self = new HumanPlayer();
        Player op = new HumanPlayer();
        BoardSpace[][] board;
        self.setColor(BoardSpace.SpaceType.BLACK);
        op.setColor(BoardSpace.SpaceType.WHITE);
        board = new BoardSpace[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                board[i][j] = new BoardSpace(i, j, BoardSpace.SpaceType.EMPTY);
            }
        }
        board[3][3].setType(BoardSpace.SpaceType.WHITE);
        board[4][4].setType(BoardSpace.SpaceType.WHITE);
        board[3][4].setType(BoardSpace.SpaceType.BLACK);
        board[4][3].setType(BoardSpace.SpaceType.BLACK);
        MCTS mcts = new MCTS(times, self, op, board);
        BoardSpace next_best = mcts.MCTS_Strategy();
        System.out.println(next_best.getX() + " " + next_best.getY());
    }

    public MCTS() {}

    public MCTS(int runTimes, Player self, Player op, BoardSpace[][] board) {
        this.times = runTimes;
        this.self = self;
        this.op = op;
        this.board = board;
    }

    public BoardSpace MCTS_Strategy() {
        MCTSNode root = new MCTSNode();
        root.setBoard(this.board);
        while (times >= 0) {
            MCTSNode next_node = select(root);
            MCTSNode simulate_node = expansion(next_node, next_node.getBoard());
            if (simulate_node == null) {
                times--;
                continue;
            }
            boolean simulation_result = simulation(simulate_node);
            backpropagation(simulate_node, simulation_result);
            times--;
        }
        int max_N = Integer.MIN_VALUE;
        MCTSNode max_node = null;
        for (int i = 0; i < root.getMctsChildren().size(); i++) {
            if (root.getMctsChildren().get(i).getTotalSimulations() >= max_N) {
                max_N = root.getMctsChildren().get(i).getTotalSimulations();
                max_node = root.getMctsChildren().get(i);
            }
        }
        BoardSpace next_step = max_node.getSpace();
        for (MCTSNode i : root.getMctsChildren()) {
            System.out.println("Number of wins: " + i.getNumberOfWin() + " " + "Number of total simulation: " + i.getTotalSimulations());
        }
        return next_step;
    }

    public double UCT(MCTSNode node) {
        if (node.getTotalSimulations() == 0) {
            return Double.POSITIVE_INFINITY;
        }
        double result = ((double) node.getNumberOfWin() / node.getTotalSimulations()) +
                (Constants.EXPLORATION_PARAM) * (Math.sqrt(Math.log(node.getParentTotalSimulation()) / node.getTotalSimulations()));
        return result;
    }


    //1. This the first step of selecting a leaf node with highest UCT value;
    public MCTSNode select(MCTSNode node) {
        if (node.getMctsChildren().isEmpty()) {
            return node;
        } else {
            MCTSNode max_node = null;
            double max_val = Double.MIN_VALUE;
            for (MCTSNode i : node.getMctsChildren()) {
                if (UCT(i) >= max_val) {
                    max_node = i;
                    max_val = UCT(i);
                }
            }
            if (max_node != null) {
                return select(max_node);
            } else {
                return node;
            }
        }
    }

    public MCTSNode expansion(MCTSNode node, BoardSpace[][] board) {
        if (!node.getMctsChildren().isEmpty()) {
            Random random = new Random();
            MCTSNode random_node = node.getMctsChildren().get(random.nextInt(0, node.getMctsChildren().size()));
            return random_node;
        }
        if (node.getDepth() % 2 == 0) {
            Map<BoardSpace, List<BoardSpace>> move = self.getAvailableMoves(board);
            if (move.isEmpty()) {
                return node;
            }
            for (BoardSpace i : move.keySet()) {
                MCTSNode temp = new MCTSNode();
                temp.setParent(node);
                temp.setBoard(futureBoard(board, i, move, self));
                temp.setDepth(node.getDepth() + 1);
                temp.setSpace(i);
                node.getMctsChildren().add(temp);
            }
        } else {
            Map<BoardSpace, List<BoardSpace>> move = op.getAvailableMoves(board);
            for (BoardSpace i : move.keySet()) {
                if (move.isEmpty()) {
                    return node;
                }
                MCTSNode temp = new MCTSNode();
                temp.setParent(node);
                temp.setBoard(futureBoard(board, i, move, op));
                temp.setDepth(node.getDepth() + 1);
                temp.setSpace(i);
                node.getMctsChildren().add(temp);
            }
        }
        if (node.getMctsChildren().isEmpty()) {
            return node;
        }
        Random random = new Random();
        MCTSNode random_node = node.getMctsChildren().get(random.nextInt(0, node.getMctsChildren().size()));
        return random_node;
    }

    public boolean simulation(MCTSNode node) {
        BoardSpace[][] cur_board = node.getBoard();
        boolean self_turn = false;
        if (node.getDepth() % 2 == 0) {
            self_turn = true;
        }
        while (!self.getAvailableMoves(cur_board).isEmpty() && !op.getAvailableMoves(cur_board).isEmpty()) {
            if (self_turn) {
                Map<BoardSpace, List<BoardSpace>> move = self.getAvailableMoves(cur_board);
                Random random = new Random();
                int random_num = random.nextInt(0, move.size());
                int counter = 0;
                BoardSpace next_space = null;
                for (BoardSpace i : move.keySet()) {
                    if (counter == random_num) {
                        next_space = i;
                        break;
                    }
                    counter++;
                }
                cur_board = futureBoard(cur_board, next_space, move, self);
                self_turn = false;
            } else {
                Map<BoardSpace, List<BoardSpace>> move = op.getAvailableMoves(cur_board);
                Random random = new Random();
                int random_num = random.nextInt(0, move.size());
                int counter = 0;
                BoardSpace next_space = null;
                for (BoardSpace i : move.keySet()) {
                    if (counter == random_num) {
                        next_space = i;
                        break;
                    }
                    counter++;
                }
                cur_board = futureBoard(cur_board, next_space, move, op);
                self_turn = true;
            }
            //System.out.println(cur_board);
        }
        int self_spaces = countSpaces(cur_board, self.getColor());
        int op_spaces = countSpaces(cur_board, op.getColor());
        if (self_spaces > op_spaces) {
            //System.out.println(self_spaces + " " + op_spaces + " " + cur_board);
            return true;
        }
        return false;
    }

    public int countSpaces(BoardSpace[][] board, BoardSpace.SpaceType type) {
        int count = 0;
        if (board == null) {
            return 0;
        }
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                if (board[i][j].getType() == type) {
                    count++;
                }
            }
        }
        return count;
    }

    public void backpropagation(MCTSNode node, boolean win) {
        if (node == null) {
            return;
        }
        if (win) {
            node.setNumberOfWin(node.getNumberOfWin() + 1);
            node.setTotalSimulations(node.getTotalSimulations() + 1);
            backpropagation(node.getParent(), win);
        } else {
            node.setTotalSimulations(node.getTotalSimulations() + 1);
            backpropagation(node.getParent(), win);
        }
    }



    public BoardSpace[][] futureBoard(BoardSpace[][] board, BoardSpace destination, Map<BoardSpace,
            List<BoardSpace>> availableMoves, Player pc) {
        BoardSpace[][] copyBoard = getCopyBoard(board);
        List<BoardSpace> theOrigins = availableMoves.get(destination);
        if (theOrigins == null || theOrigins.isEmpty()) {
            System.out.println("Error: Null");
            return null;
        }
        for (BoardSpace eachOrigin : theOrigins) {
            // First find the direction to the destination
            int rowChange = Integer.compare(destination.getX(), eachOrigin.getX());
            int columnChange = Integer.compare(destination.getY(), eachOrigin.getY());
            // Then actually take the steps
            int newRow = eachOrigin.getX();
            int newColumn = eachOrigin.getY();
            while (true) {
                //Take space for each boardspace
                BoardSpace thePosition = copyBoard[newRow][newColumn];
                // If already reached the destination, break
                if (thePosition.getType() != pc.getColor()) {
                    thePosition.setType(pc.getColor());
                }
                if (newRow == destination.getX() &&
                        newColumn == destination.getY()) {
                    break;
                }
                // Else, continue moving in that direction towards the destination
                newRow += rowChange;
                newColumn += columnChange;
            }
        }
        return copyBoard;
    }

    public BoardSpace[][] getCopyBoard(BoardSpace[][] original) {
        BoardSpace[][] copyBoard = new BoardSpace[original.length][original[0].length];
        for (int i = 0; i < copyBoard.length; i++) {
            for (int j = 0; j < copyBoard[0].length; j++) {
                copyBoard[i][j] = new BoardSpace(original[i][j]);
            }
        }
        return copyBoard;
    }

    public String toString(BoardSpace[][] board) {
        String board_str = "";
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                board_str += board[i][j].getType().toString().toLowerCase() + "  |";
            }
            board_str += "\n";
        }
        return board_str;
    }

}