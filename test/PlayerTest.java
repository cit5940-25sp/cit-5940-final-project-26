import othello.gamelogic.*;

import java.util.*;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for OthelloGame
 * JUnit 4.13.2
 */
public class PlayerTest {
    private BoardSpace[][] board;
    private final Player black = new HumanPlayer();

    /**
     * identical test fixture for each test – reproducible and small
     */
    @Before
    public void initBoard() {
        // prepare a fresh, empty 8×8 board and a black player
        board = new BoardSpace[8][8];
        for (int x = 0; x < 8; x++)
            for (int y = 0; y < 8; y++)
                board[x][y] = new BoardSpace(x, y, BoardSpace.SpaceType.EMPTY);
        black.setColor(BoardSpace.SpaceType.BLACK);
    }

    @Test
    public void openingMovesTest() {
        // verify black's four opening moves around the centre
        board[3][3].setType(BoardSpace.SpaceType.WHITE);
        board[4][4].setType(BoardSpace.SpaceType.WHITE);
        board[3][4].setType(BoardSpace.SpaceType.BLACK);
        board[4][3].setType(BoardSpace.SpaceType.BLACK);

        Map<BoardSpace, List<BoardSpace>> moves = black.getAvailableMoves(board);

        assertEquals(
                Set.of(board[2][3], board[3][2], board[4][5], board[5][4]),
                moves.keySet()
        );
    }

    @Test
    public void verticalFlipTest() {
        // vertical flip
        board[2][3].setType(BoardSpace.SpaceType.BLACK);
        board[3][3].setType(BoardSpace.SpaceType.WHITE);
        board[4][3].setType(BoardSpace.SpaceType.WHITE);
        board[5][3].setType(BoardSpace.SpaceType.WHITE);

        Map<BoardSpace, List<BoardSpace>> moves = black.getAvailableMoves(board);

        assertEquals(Set.of(board[6][3]), moves.keySet());
    }

    @Test
    public void horizontalFlipTest() {
        // horizontal flip
        board[3][2].setType(BoardSpace.SpaceType.BLACK);
        board[3][3].setType(BoardSpace.SpaceType.WHITE);
        board[3][4].setType(BoardSpace.SpaceType.WHITE);
        board[3][5].setType(BoardSpace.SpaceType.WHITE);

        Map<BoardSpace, List<BoardSpace>> moves = black.getAvailableMoves(board);

        assertEquals(Set.of(board[3][6]), moves.keySet());
    }

    @Test
    public void diagonalFlipTest() {
        // single-direction flip along the NW→SE diagonal
        board[1][1].setType(BoardSpace.SpaceType.BLACK);
        board[2][2].setType(BoardSpace.SpaceType.WHITE);
        board[3][3].setType(BoardSpace.SpaceType.WHITE);
        board[4][4].setType(BoardSpace.SpaceType.WHITE);

        Map<BoardSpace, List<BoardSpace>> moves = black.getAvailableMoves(board);

        assertEquals(Set.of(board[5][5]), moves.keySet());
        // ensure the flip origin is exactly the anchor
        assertEquals(List.of(board[1][1]), moves.get(board[5][5]));
    }

    @Test
    public void splitChainTest() {
        // split chain: B-W- -W-B in a row
        board[2][4].setType(BoardSpace.SpaceType.BLACK);
        board[3][4].setType(BoardSpace.SpaceType.WHITE);
        board[5][4].setType(BoardSpace.SpaceType.WHITE);
        board[6][4].setType(BoardSpace.SpaceType.BLACK);

        Map<BoardSpace,List<BoardSpace>> moves = black.getAvailableMoves(board);

        // only the gap at (4,4) is legal
        assertEquals(Set.of(board[4][4]), moves.keySet());
        // both anchors (2,4) and (6,4) should appear as origins
        List<BoardSpace> origins = moves.get(board[4][4]);
        assertTrue(origins.contains(board[2][4]));
        assertTrue(origins.contains(board[6][4]));
        assertEquals(2, origins.size());
    }

    @Test
    public void multiDirectionFlipTest() {
        // two chains intersecting at (3,3):
        // vertical chain: B at (1,3), W at (2,3)
        // horizontal chain: B at (3,1), W at (3,2)
        board[1][3].setType(BoardSpace.SpaceType.BLACK);
        board[2][3].setType(BoardSpace.SpaceType.WHITE);
        board[3][1].setType(BoardSpace.SpaceType.BLACK);
        board[3][2].setType(BoardSpace.SpaceType.WHITE);

        Map<BoardSpace,List<BoardSpace>> moves = black.getAvailableMoves(board);

        // only (3,3) is legal
        assertEquals(Set.of(board[3][3]), moves.keySet());
        // both vertical and horizontal anchors
        List<BoardSpace> origins = moves.get(board[3][3]);
        assertTrue(origins.contains(board[1][3]));
        assertTrue(origins.contains(board[3][1]));
        assertEquals(2, origins.size());
    }

    @Test
    public void noMovesTest() {
        // empty board should produce no legal moves
        Map<BoardSpace, List<BoardSpace>> moves = black.getAvailableMoves(board);
        assertTrue(moves.isEmpty());
    }

    @Test
    public void singleColorNoFlankTest() {
        // chain of whites without flanking blacks → no moves
        board[0][3].setType(BoardSpace.SpaceType.WHITE);
        board[1][3].setType(BoardSpace.SpaceType.WHITE);
        board[2][3].setType(BoardSpace.SpaceType.WHITE);

        Map<BoardSpace, List<BoardSpace>> moves = black.getAvailableMoves(board);
        assertTrue(moves.isEmpty());
    }

    @Test
    public void edgeWallsNoFlankTest() {
        // both left and right wall pure‐white chains should yield no moves
        int[][] wallCols = {{0,4,1,4,2,4},  // (x,y) = (0,4),(1,4),(2,4) on left
                {7,2,6,2,5,2}}; // (x,y) = (7,2),(6,2),(5,2) on right
        for (int[] pos : wallCols) {
            // reset board to empty
            for (BoardSpace[] row : board)
                for (BoardSpace c : row)
                    c.setType(BoardSpace.SpaceType.EMPTY);

            // place a W-W-W chain hugging the wall
            board[pos[0]][pos[1]].setType(BoardSpace.SpaceType.WHITE);
            board[pos[2]][pos[3]].setType(BoardSpace.SpaceType.WHITE);
            board[pos[4]][pos[5]].setType(BoardSpace.SpaceType.WHITE);

            Map<BoardSpace,List<BoardSpace>> moves = black.getAvailableMoves(board);
            assertTrue(
                    "No moves expected when only white chain at wall",
                    moves.isEmpty()
            );
        }
    }
}