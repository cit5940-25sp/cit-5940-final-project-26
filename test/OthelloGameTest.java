import othello.gamelogic.*;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;
import static othello.gamelogic.BoardSpace.SpaceType.EMPTY;

/**
 * Unit tests for OthelloGame
 * JUnit 4.13.2
 */
public class OthelloGameTest {

    private Player black;     // acts first
    private Player white;
    private OthelloGame game;
    private BoardSpace[][] board;

    /**
     * identical test fixture for each test – reproducible and small
     */
    @Before
    public void setUp() {
        black = new HumanPlayer();
        white = new HumanPlayer();
        black.setColor(BoardSpace.SpaceType.BLACK);
        white.setColor(BoardSpace.SpaceType.WHITE);
        game = new OthelloGame(black, white);
        board = game.getBoard();
    }

    @Test
    public void gameOthelloBasicsTest() {
        Map<BoardSpace, List<BoardSpace>> moves = game.getAvailableMoves(black);
        // expected squares in reading-order
        Set<BoardSpace> expected = new HashSet<>(Arrays.asList(
                board[2][3], board[3][2], board[4][5], board[5][4]));

        assertEquals("black should have 4 legal moves on move 1",
                4, moves.size());
        assertTrue(moves.keySet().containsAll(expected));
    }

    @Test
    public void gameOthelloFlipTest() {
        // Pick the opening move at (2,3), which should flip (3,3)
        BoardSpace dest = board[2][3];

        Map<BoardSpace, List<BoardSpace>> moves =
                game.getAvailableMoves(black);
        assertTrue(moves.containsKey(dest));

        game.takeSpaces(black, white, moves, dest);
        // After the move, (2,3), (3,3), and (4,3) must all be BLACK
        Set<BoardSpace> blacks = Set.of(board[2][3], board[3][3], board[4][3]);
        for (BoardSpace c : blacks) {
            assertSame(BoardSpace.SpaceType.BLACK, c.getType());
        }
        assertSame(BoardSpace.SpaceType.WHITE, board[4][4].getType());

        Map<BoardSpace, List<BoardSpace>> next = game.getAvailableMoves(white);
        Set<BoardSpace> expected = Set.of(board[2][2], board[2][4], board[4][2]);
        assertEquals(3, next.size());
        assertTrue(next.keySet().containsAll(expected));
    }

    @Test
    public void frontFlipAt23() {
        // Arrange: build custom column and compute moves
        // clear center four
        for (int x=3; x<=4; x++)
            for (int y=3; y<=4; y++)
                board[x][y].setType(EMPTY);


    }


}