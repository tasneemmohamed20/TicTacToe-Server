package models;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * FXML Controller class
 *
 * @author HP
 */


public class GameModel {
    private final String gameId;
    private final String player1;
    private final String player1Symbol;
    private final String player2;
    private final String player2Symbol;

    private String[] board;
    private String currentPlayer;
    private boolean isPlayerTurn;

    private transient final Gson gson = new Gson();

    public GameModel(String gameId, String player1, String player1Symbol, String player2, String player2Symbol) {
        if (player1 == null || player2 == null || player1.isEmpty() || player2.isEmpty()) {
            throw new IllegalArgumentException("Player names cannot be null or empty.");
        }
        if (player1.equals(player2)) {
            throw new IllegalArgumentException("Players must have different names.");
        }

        this.gameId = gameId;
        this.player1 = player1;
        this.player1Symbol = player1Symbol;
        this.player2 = player2;
        this.player2Symbol = player2Symbol;

        this.board = new String[9];
        Arrays.fill(this.board, null);

        this.currentPlayer = player1;
        this.isPlayerTurn = player1Symbol.equals("X");
    }

    public String getGameId() {
        return gameId;
    }

    public String getPlayer1() {
        return player1;
    }

    public String getPlayer1Symbol() {
        return player1Symbol;
    }

    public String getPlayer2() {
        return player2;
    }

    public String getPlayer2Symbol() {
        return player2Symbol;
    }

    public String[] getBoard() {
        return board.clone();
    }

    public String getCurrentPlayer() {
        return currentPlayer;
    }

    public boolean isPlayerTurn() {
        return isPlayerTurn;
    }

    public void setIsPlayerTurn(boolean isPlayerTurn) {
        this.isPlayerTurn = isPlayerTurn;
    }

    public boolean makeMove(String cellId, String symbol) {
        int cellIndex = getCellIndex(cellId);
        System.out.println("[DEBUG] Attempting move - Cell: " + cellId + ", Symbol: " + symbol);
    
        if (cellIndex < 0 || cellIndex >= board.length || board[cellIndex] != null) {
            System.err.println("Invalid move: Cell " + cellId + " is already occupied or out of bounds.");
            return false;
        }
    
        String currentSymbol = currentPlayer.equals(player1) ? player1Symbol : player2Symbol;
        if (!symbol.equals(currentSymbol)) {
            System.err.println("Invalid move: Symbol " + symbol + " does not match current player " + currentPlayer);
            return false;
        }
    
        board[cellIndex] = symbol;
        switchTurn();
        return true;
    }

    public String checkGameOver() {
        if (isWinningConditionMet(player1Symbol)) {
            return player1 + " wins!";
        } else if (isWinningConditionMet(player2Symbol)) {
            return player2 + " wins!";
        } else if (isBoardFull()) {
            return "It's a draw!";
        }
        return "Game ongoing"; // Game continues
    }

    public void resetBoard() {
        Arrays.fill(board, null);
        currentPlayer = player1;
        isPlayerTurn = player1Symbol.equals("X");
    }

    public String createMoveRequest(String cellId) {
        Map<String, String> moveData = new HashMap<>();
        moveData.put("gameId", gameId);
        moveData.put("cell", cellId);
        moveData.put("symbol", currentPlayer.equals(player1) ? player1Symbol : player2Symbol);

        return gson.toJson(new RequsetModel("move", moveData));
    }

    private boolean isWinningConditionMet(String symbol) {
        int[][] winningCombinations = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8}, // Rows
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8}, // Columns
            {0, 4, 8}, {2, 4, 6}             // Diagonals
        };

        for (int[] combination : winningCombinations) {
            if (symbol.equals(board[combination[0]]) &&
                symbol.equals(board[combination[1]]) &&
                symbol.equals(board[combination[2]])) {
                return true;
            }
        }
        return false;
    }

    private boolean isBoardFull() {
        return Arrays.stream(board).allMatch(cell -> cell != null);
    }

    private int getCellIndex(String cellId) {
        switch (cellId) {
            case "cell1": return 0;
            case "cell2": return 1;
            case "cell3": return 2;
            case "cell4": return 3;
            case "cell5": return 4;
            case "cell6": return 5;
            case "cell7": return 6;
            case "cell8": return 7;
            case "cell9": return 8;
            default: return -1;
        }
    }

    private void switchTurn() {
        currentPlayer = currentPlayer.equals(player1) ? player2 : player1;
        isPlayerTurn = !isPlayerTurn;
        System.out.println("[DEBUG] Turn switched - Current player: " + currentPlayer 
        + ", Symbol: " + (currentPlayer.equals(player1) ? player1Symbol : player2Symbol)
        + ", isPlayerTurn: " + isPlayerTurn);
    }
    
    

}