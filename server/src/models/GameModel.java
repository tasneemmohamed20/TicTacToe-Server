package models;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * FXML Controller class
 *
 * @author HP
 */
public class GameModel {
    private String player1;
    private String player2;
    private String[] board;
    private String currentPlayer; 

    public GameModel(String player1, String player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.board = new String[9]; 
        Arrays.fill(this.board, null); 
        this.currentPlayer = player1;
    }

    public String[] getBoardState() {
        return board;
    }

    public boolean makeMove(String player, String cellIndex) {
        int cell = Integer.parseInt(cellIndex) - 1; 
        if (cell < 0 || cell >= 9 || board[cell] != null || !player.equals(currentPlayer)) {
            return false; 
        }
        board[cell] = player; 
        currentPlayer = currentPlayer.equals(player1) ? player2 : player1; 
        return true;
    }

    public String checkGameState() {
        int[][] winningCombinations = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8}, 
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8}, 
            {0, 4, 8}, {2, 4, 6}            
        };

        for (int[] combination : winningCombinations) {
            if (board[combination[0]] != null &&
                board[combination[0]].equals(board[combination[1]]) &&
                board[combination[1]].equals(board[combination[2]])) {
                return board[combination[0]] + " wins!";
            }
        }

        boolean isBoardFull = Arrays.stream(board).allMatch(cell -> cell != null);
        if (isBoardFull) {
            return "draw";
        }

        return "ongoing"; 
    }
}