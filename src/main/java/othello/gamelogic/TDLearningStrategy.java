package othello.gamelogic;

import java.util.Map;
import java.util.List;

public class TDLearningStrategy implements AIStrategy {

    /**
     *
     * @param board the Othello game board
     * @param actingPlayer the computer player that is moving
     * @return the determined board position for the computer player
     */
    @Override
    public BoardSpace computerMove(BoardSpace[][] board, Player actingPlayer) {
        // Obtain all available moves
        Map<BoardSpace, List<BoardSpace>> availableMoves = actingPlayer.getAvailableMoves(board);
        if (availableMoves == null || availableMoves.isEmpty()) {
            return null;
        }

        return availableMoves.keySet().iterator().next();
    }
}
