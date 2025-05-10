package othello.gamelogic;

import java.util.ArrayList;

public class Node {

    private BoardSpace boardspace;
    private int weight;
    private Node parent;
    private ArrayList<Node> children = new ArrayList<>();
    private boolean isRoot = false;
    private boolean max = false;
    private boolean min = false;
    private int depth;
    private boolean visited = false;

    public Node() {}

    public void setBoardSpace(BoardSpace bs) {
        this.boardspace = bs;
    }

    public BoardSpace getBoardspace() {
        return this.boardspace;
    }

    public void setWeight(int wt) {
        this.weight = wt;
    }

    public int getWeight() {
        return this.weight;
    }

    public void setChildren(ArrayList<Node> ref) {
        this.children = ref;
    }

    public ArrayList<Node> getChildren() {
        return this.children;
    }

    public boolean isRoot() {
        return isRoot;
    }

    public void setRoot(boolean root) {
        isRoot = root;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public Node getParent() {
        return this.parent;
    }

    public void setMin(boolean min) {
        this.min = min;
    }

    public boolean isMin() {
        return this.min;
    }

    public void setMax(boolean max) {
        this.max = max;
    }

    public boolean isMax() {
        return this.max;
    }

    public void setDepth(int d) {
        this.depth = d;
    }

    public int getDepth() {
        return this.depth;
    }

    public void setVisited(boolean v) {
        this.visited = v;
    }

    public boolean isVisited() {
        return this.visited;
    }
}
