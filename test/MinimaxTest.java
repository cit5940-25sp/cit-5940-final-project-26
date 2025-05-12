import othello.Constants;
import othello.gamelogic.*;

import java.util.*;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class MinimaxTest {

    private BoardSpace[][] board;
    private final Player black = new HumanPlayer();
    private final Player white = new HumanPlayer();
    private Minimax mm;

    @Before
    public void setup() {
        mm = new Minimax();
        board = new BoardSpace[8][8];
        for (int x = 0; x < 8; x++)
            for (int y = 0; y < 8; y++)
                board[x][y] = new BoardSpace(x, y, BoardSpace.SpaceType.EMPTY);

        board[3][3].setType(BoardSpace.SpaceType.WHITE);
        board[4][4].setType(BoardSpace.SpaceType.WHITE);
        board[3][4].setType(BoardSpace.SpaceType.BLACK);
        board[4][3].setType(BoardSpace.SpaceType.BLACK);

        black.setColor(BoardSpace.SpaceType.BLACK);
        white.setColor(BoardSpace.SpaceType.WHITE);
    }



    @Test
    public void weightEvalTest() {
        // score increases by cell weight when black adds piece
        int base = mm.computeWeight(board, black);
        board[2][3].setType(BoardSpace.SpaceType.BLACK);
        int after = mm.computeWeight(board, black);
        assertTrue(after > base);
        assertEquals(Constants.BOARD_WEIGHTS[2][3], after - base);
    }

    @Test
    public void twoLayerChoiceTest() {
        // B-W-W-W vertical: choose rightmost
        board[1][3].setType(BoardSpace.SpaceType.BLACK);
        board[2][3].setType(BoardSpace.SpaceType.WHITE);
        board[3][3].setType(BoardSpace.SpaceType.WHITE);
        board[4][3].setType(BoardSpace.SpaceType.WHITE);
        Node root = new Node();
        mm.buildTree(board, black, white, 0, root, 2);
        BoardSpace best = mm.minimaxStrategyWithMaxDepth(root);
        assertEquals(5, best.getX());
        assertEquals(3, best.getY());
    }

    @Test
    public void emptyBoardLeafTest() {
        // empty board yields leaf with zero weight
        BoardSpace[][] empty = new BoardSpace[8][8];
        for (int x = 0; x < 8; x++)
            for (int y = 0; y < 8; y++)
                empty[x][y] = new BoardSpace(x, y, BoardSpace.SpaceType.EMPTY);
        Node root = new Node();
        mm.buildTree(empty, black, white, 0, root, 2);
        assertTrue(root.getChildren().isEmpty());
        assertEquals(0, root.getWeight());
    }

    @Test
    public void noMoveTest() {
        // when no legal moves, node is leaf
        BoardSpace[][] empty = new BoardSpace[8][8];
        for (int x = 0; x < 8; x++)
            for (int y = 0; y < 8; y++)
                empty[x][y] = new BoardSpace(x, y, BoardSpace.SpaceType.EMPTY);
        Node root = new Node();
        mm.buildTree(empty, black, white, 0, root, 2);
        assertTrue(root.getChildren().isEmpty());
        assertEquals(0, root.getWeight());
    }

    @Test
    public void emptyWeightTest() {
        // computeWeight on empty board
        BoardSpace[][] empty = new BoardSpace[8][8];
        for (int x = 0; x < 8; x++)
            for (int y = 0; y < 8; y++)
                empty[x][y] = new BoardSpace(x, y, BoardSpace.SpaceType.EMPTY);
        int w = mm.computeWeight(empty, black);
        assertEquals(0, w);
    }

    @Test
    public void mainCoverageTest() {
        // invoke main() for coverage
        Minimax.main(new String[0]);
    }

    @Test
    public void depthLeafTest() {
        // at maxDepth direct children are leaves
        Node root = new Node();
        mm.buildTree(board, black, white, 0, root, 2);
        for (Node leaf : root.getChildren()) {
            assertTrue(leaf.getChildren().isEmpty());
            assertNotEquals(0, leaf.getWeight());
        }
    }

    @Test
    public void opponentNoMoveTest() {
        // opponent has no reply move
        BoardSpace[][] custom = new BoardSpace[8][8];
        for (int x = 0; x < 8; x++)
            for (int y = 0; y < 8; y++)
                custom[x][y] = new BoardSpace(x, y, BoardSpace.SpaceType.EMPTY);
        custom[3][3].setType(BoardSpace.SpaceType.WHITE);
        custom[3][4].setType(BoardSpace.SpaceType.BLACK);
        BoardSpace best = mm.minimaxOneStep(custom, black);
        assertNotNull(best);
        assertEquals(3, best.getX());
        assertEquals(2, best.getY());
    }


    @Test
    public void copyPrintTest() {
        // getCopyBoard and toString consistency
        BoardSpace[][] copy = mm.getCopyBoard(board);
        assertNotSame(board, copy);
        assertEquals(mm.toString(board), mm.toString(copy));
    }

    @Test
    public void verticalFlipTest() {
        // verify vertical flip logic
        Map<BoardSpace, List<BoardSpace>> moves = black.getAvailableMoves(board);
        BoardSpace dst = board[2][3];
        BoardSpace[][] next = mm.futureBoard(board, dst, moves, black);
        for (int x : new int[]{2, 3, 4}) {
            assertSame(BoardSpace.SpaceType.BLACK, next[x][3].getType());
        }
    }

    @Test
    public void oneStepNoMoveTest() {
        BoardSpace[][] empty = new BoardSpace[8][8];
        for (int x=0;x<8;x++) for (int y=0;y<8;y++)
            empty[x][y] = new BoardSpace(x,y,BoardSpace.SpaceType.EMPTY);
        assertNull(mm.minimaxOneStep(empty, black));
    }

    @Test
    public void copyIsolationTest() {
        BoardSpace[][] c = mm.getCopyBoard(board);
        c[3][3].setType(BoardSpace.SpaceType.EMPTY); // mutate copy
        assertNotEquals(mm.toString(board), mm.toString(c)); // originals differ
    }

    @Test
    public void futureBoardNullOriginTest() {
        Map<BoardSpace,List<BoardSpace>> moves = black.getAvailableMoves(board);
        BoardSpace fake = new BoardSpace(0,0,BoardSpace.SpaceType.EMPTY); // not a legal move
        assertNull(mm.futureBoard(board, fake, moves, black));
    }
}
