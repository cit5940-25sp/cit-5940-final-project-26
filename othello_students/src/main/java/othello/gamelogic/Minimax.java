package othello.gamelogic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.sun.source.tree.Tree;
import javafx.scene.paint.Color;
import othello.Constants;

public class Minimax {

    private Node root;

    public static void main(String[] args) {
        Minimax test = new Minimax();
        BoardSpace[][] board;
        Player pc = new HumanPlayer();
        pc.setColor(BoardSpace.SpaceType.BLACK);
        board = new BoardSpace[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                board[i][j] = new BoardSpace(i, j, BoardSpace.SpaceType.EMPTY);
            }
        }
        /*board[2][3].setType(BoardSpace.SpaceType.BLACK);
        board[3][3].setType(BoardSpace.SpaceType.BLACK);
        board[4][3].setType(BoardSpace.SpaceType.BLACK);
        board[1][4].setType(BoardSpace.SpaceType.BLACK);
        board[0][5].setType(BoardSpace.SpaceType.BLACK);
        board[2][5].setType(BoardSpace.SpaceType.BLACK);
        board[2][4].setType(BoardSpace.SpaceType.WHITE);
        board[3][4].setType(BoardSpace.SpaceType.WHITE);
        board[4][4].setType(BoardSpace.SpaceType.WHITE);

         */
        board[3][3].setType(BoardSpace.SpaceType.WHITE);
        board[3][4].setType(BoardSpace.SpaceType.BLACK);
        board[4][3].setType(BoardSpace.SpaceType.BLACK);
        board[4][4].setType(BoardSpace.SpaceType.WHITE);
        //System.out.println(test.toString(board));

        /*Map<BoardSpace, List<BoardSpace>> moves = pc.getAvailableMoves(board);
        //System.out.println(moves.get())
        Player one = new HumanPlayer();
        Player two = new ComputerPlayer("");
        test.buildTree(board, one, two);

         */
        Node root = new Node();
        Player one = new HumanPlayer();
        Player two = new ComputerPlayer("");
        one.setColor(BoardSpace.SpaceType.BLACK);
        two.setColor(BoardSpace.SpaceType.WHITE);
        test.buildTree(board, two, one, 0, root, 3);
        //System.out.println(root.getChildren());
        for (Node i : root.getChildren()) {
            System.out.println("The i is : " + i.getBoardspace().getX() + " " + i.getBoardspace().getY());
            System.out.println(i.isMax());
            for (Node j : i.getChildren()) {
                System.out.println("The next move i + 1: " + j.getBoardspace().getX() + " " + j.getBoardspace().getY());
            }
        }
    }

    public Minimax() {}


    /*

    public BoardSpace miniMaxStrategy(Node node) {
        if (!node.isRoot()) {
            System.out.println("Invalid root node!");
            return null;
        }

        for (Node root_child : node.getChildren()) {
            if (root_child.getChildren().isEmpty()) {
                continue;
            }
            Node min_node = findMin(root_child.getChildren());
            root_child.setWeight(min_node.getWeight());
        }
        Node max_node = findMax(node.getChildren());
        return max_node.getBoardspace();
    }

    public Node findMax(ArrayList<Node> children) {
        Node max_node = children.get(0);
        int max = max_node.getWeight();
        for (Node n : children) {
            if (n.getWeight() >= max) {
                max_node = n;
                max = n.getWeight();
            }
        }
        return max_node;
    }

    public Node findMin(ArrayList<Node> children) {
        Node min_node = children.get(0);
        int min = min_node.getWeight();
        for (Node n : children) {
            if (n.getWeight() <= min) {
                min_node = n;
                min = n.getWeight();
            }
        }
        return min_node;
    }


     */


    //This method is the next step for buildTree
    //Return the next optimal BoardSpace to move based on minimax algo
    public BoardSpace minimaxStrategyWithMaxDepth(Node node) {
        Node next_node = value(node);
        BoardSpace next_move = next_node.getBoardspace();
        return next_move;
    }

    //If isMin, then we need to find the min value of all its children
    //If isMax, then we need to find the max value of all its children
    public Node value(Node node) {
        if (node.getChildren().isEmpty()) {
            return node;
        }
        if (node.isMax()) {
            return max_node(node);
        } else if (node.isMin()){
            return min_node(node);
        }
        return node;
    }

    public Node max_node(Node node) {
        int max = Integer.MIN_VALUE;
        Node max_node = null;
        if (node.getChildren().isEmpty()) {
            return node;
        }
        for (Node successor : node.getChildren()) {
            if (value(successor).getWeight() >= max) {
                max = value(successor).getWeight();
                max_node = successor;
                max_node.setWeight(max);
            }
        }
        return max_node;
    }

    public Node min_node(Node node) {
        int min = Integer.MAX_VALUE;
        Node min_node = null;
        if (node.getChildren().isEmpty()) {
            return node;
        }
        for (Node successor : node.getChildren()) {
            if (value(successor).getWeight() <= min) {
                min = value(successor).getWeight();
                min_node = successor;
                min_node.setWeight(min);
            }
        }
        return min_node;
    }


    //Depth start from 0
    //This method is to build the future state tree from current board state
    public void buildTree(BoardSpace[][] board, Player pc, Player op, int depth, Node root, int maxDepth) {
        BoardSpace[][] copyBoard = this.getCopyBoard(board);
        if (depth > maxDepth) {
            return;
        }
        if (depth == 0) {
            root.setRoot(true);
            root.setDepth(depth);
            root.setMax(true);
            buildTree(board, pc, op, depth + 1, root, maxDepth);
            return;
        }
        if (depth % 2 != 0) {
            //This is pc
            Map<BoardSpace, List<BoardSpace>> availableMoves = pc.getAvailableMoves(copyBoard);
            if (availableMoves.isEmpty()) {
                root.setWeight(computeWeight(copyBoard,pc));
                return;
            }
            for (BoardSpace i : availableMoves.keySet()) {
                Node temp = new Node();
                temp.setBoardSpace(i);
                temp.setDepth(depth);
                temp.setMin(true);
                BoardSpace[][] future = futureBoard(copyBoard,i,availableMoves,pc);
                root.getChildren().add(temp);
                buildTree(future,pc,op,depth + 1,temp, maxDepth);
            }
        } else {
            //This is op
            Map<BoardSpace, List<BoardSpace>> availableMoves = op.getAvailableMoves(copyBoard);
            if (availableMoves.isEmpty()) {
                root.setWeight(computeWeight(copyBoard,pc));
                return;
            }
            for (BoardSpace i : availableMoves.keySet()) {
                Node temp = new Node();
                temp.setBoardSpace(i);
                temp.setDepth(depth);
                temp.setMax(true);
                BoardSpace[][] future = futureBoard(copyBoard,i,availableMoves,op);
                root.getChildren().add(temp);
                buildTree(future,pc,op,depth + 1,temp, maxDepth);
            }
        }
    }


    /*
    //Build a game tree with max depth to be 2, and it can predict the next best option
    public Node buildTree2(BoardSpace[][] board, Player pc, Player op) {
        //We need to get the copy of original board to predict
        BoardSpace[][] copyBoard = this.getCopyBoard(board);
        //The minimax strategy will be preformed below to get next BoardSpace to go
        Map<BoardSpace, List<BoardSpace>> availableMoves = pc.getAvailableMoves(copyBoard);
        //If there is no available move, return null
        Node root = new Node();
        root.setRoot(true);
        root.setDepth(0);
        ArrayList<Node> root_children = new ArrayList<>();
        for (BoardSpace space : availableMoves.keySet()) {
            Node tempNode = new Node();
            tempNode.setBoardSpace(space);
            tempNode.setDepth(1);
            tempNode.setParent(root);
            tempNode.setMax(true);
            BoardSpace[][] future = futureBoard(copyBoard,space,availableMoves,pc);
            tempNode.setWeight(computeWeight(future,pc));
            Map<BoardSpace, List<BoardSpace>> availableMoves_op = op.getAvailableMoves(future);
            ArrayList<Node> children = new ArrayList<>();
            for (BoardSpace space_op : availableMoves_op.keySet()) {
                Node op_node = new Node();
                op_node.setBoardSpace(space_op);
                op_node.setDepth(2);
                op_node.setParent(tempNode);
                op_node.setMin(true);
                BoardSpace[][] future_op = futureBoard(future, space_op, availableMoves_op, op);
                int weight_op = computeWeight(future_op, pc);
                op_node.setWeight(weight_op);
                children.add(op_node);
            }
            tempNode.setChildren(children);
            root_children.add(tempNode);
        }
        root.setChildren(root_children);
        return root;
    }

     */


    //This method is for creating a copy of current state of board
    //In order to prevent any modification for original board
    public BoardSpace[][] getCopyBoard(BoardSpace[][] original) {
        BoardSpace[][] copyBoard = new BoardSpace[original.length][original[0].length];
        for (int i = 0; i < copyBoard.length; i++) {
            for (int j = 0; j < copyBoard[0].length; j++) {
                copyBoard[i][j] = new BoardSpace(original[i][j]);
            }
        }
        return copyBoard;
    }

    public int computeWeight(BoardSpace[][] board, Player pc) {
        int self_score = 0;
        int opponent_score = 0;
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                if (board[i][j].getType() == pc.getColor()) {
                    self_score += Constants.BOARD_WEIGHTS[i][j];
                } else if (board[i][j].getType() == BoardSpace.SpaceType.EMPTY) {
                    //Do nothing
                } else {
                    opponent_score += Constants.BOARD_WEIGHTS[i][j];
                }
            }
        }
        return (self_score - opponent_score);
    }

    public BoardSpace[][] futureBoard(BoardSpace[][] board, BoardSpace destination, Map<BoardSpace,
            List<BoardSpace>> availableMoves, Player pc) {
        BoardSpace[][] copyBoard = getCopyBoard(board);
        List<BoardSpace> theOrigins = availableMoves.get(destination);
        if (theOrigins == null || theOrigins.isEmpty()) {
            System.out.println("Origins is null");
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
