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
    public void gamePlayerOpeningMovesTest() {
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
    public void gamePlayerVerticalFlipTest() {
        // single-direction flip: a vertical north chain
        board[2][3].setType(BoardSpace.SpaceType.BLACK);
        board[3][3].setType(BoardSpace.SpaceType.WHITE);
        board[4][3].setType(BoardSpace.SpaceType.WHITE);
        board[5][3].setType(BoardSpace.SpaceType.WHITE);

        Map<BoardSpace, List<BoardSpace>> moves = black.getAvailableMoves(board);

        assertEquals(Set.of(board[6][3]), moves.keySet());
    }

    @Test
    public void gamePlayerDiagonalFlipTest() {
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
    public void gamePlayerNoMovesTest() {
        // empty board should produce no legal moves
        Map<BoardSpace, List<BoardSpace>> moves = black.getAvailableMoves(board);
        assertTrue(moves.isEmpty());
    }

    @Test
    public void gamePlayerSingleColorNoFlankTest() {
        // chain of whites without flanking blacks → no moves
        board[0][3].setType(BoardSpace.SpaceType.WHITE);
        board[1][3].setType(BoardSpace.SpaceType.WHITE);
        board[2][3].setType(BoardSpace.SpaceType.WHITE);

        Map<BoardSpace, List<BoardSpace>> moves = black.getAvailableMoves(board);
        assertTrue(moves.isEmpty());
    }
}