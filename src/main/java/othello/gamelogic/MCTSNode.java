package othello.gamelogic;

import java.util.ArrayList;

public class MCTSNode {
    private BoardSpace space;
    private BoardSpace[][] board;
    private int numberOfWin;
    private int totalSimulations;
    private int parentTotalSimulation;
    private MCTSNode parent;
    private boolean isLeaf = false;
    private ArrayList<MCTSNode> mctsChildren = new ArrayList<>();
    private int depth;


    public void setMctsChildren(ArrayList<MCTSNode> children) {
        this.mctsChildren = children;
    }

    public ArrayList<MCTSNode> getMctsChildren() {
        return this.mctsChildren;
    }


    public void setIsLeaf(boolean leaf) {
        this.isLeaf = leaf;
    }

    public boolean isLeaf() {
        return this.isLeaf;
    }

    public void setNumberOfWin(int num) {
        this.numberOfWin = num;
    }

    public int getNumberOfWin() {
        return this.numberOfWin;
    }

    public void setTotalSimulations(int num) {
        this.totalSimulations = num;
    }

    public int getTotalSimulations() {
        return this.totalSimulations;
    }

    public void setParent(MCTSNode node) {
        this.parent = node;
    }

    public MCTSNode getParent() {
        return this.parent;
    }

    public int getParentTotalSimulation() {
        if (this.parent != null) {
            return this.parent.getTotalSimulations();
        }
        return 0;
    }

    public void setBoard(BoardSpace[][] board) {
        this.board = board;
    }

    public BoardSpace[][] getBoard() {
        return this.board;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public int getDepth() {
        return this.depth;
    }

    public void setSpace(BoardSpace space) {
        this.space = space;
    }

    public BoardSpace getSpace() {
        return this.space;
    }
}