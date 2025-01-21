/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network;

import java.util.Arrays;
import models.GameModel;
import models.ResponsModel;

/**
 *
 * @author Ism
 */
public class GameThread extends Thread {
    private final ClientHandler playerOne;
    private final ClientHandler playerTwo;
    private final GameModel gameModel;
    private boolean isGameRunning = true;

    public GameThread(ClientHandler playerOne, ClientHandler playerTwo, GameModel gameModel) {
        this.playerOne = playerOne;
        this.playerTwo = playerTwo;
        this.gameModel = gameModel;
    }

    @Override
    public void run() {
        try {
            playerOne.sendMessage(new ResponsModel("gameStart", "Game started! You are Player X.", gameModel));
            playerTwo.sendMessage(new ResponsModel("gameStart", "Game started! You are Player O.", gameModel));

            while (isGameRunning) {
                ClientHandler currentPlayer = gameModel.isPlayerTurn() ? playerOne : playerTwo;
                ClientHandler opponentPlayer = gameModel.isPlayerTurn() ? playerTwo : playerOne;

                currentPlayer.sendMessage(new ResponsModel("info", "Your turn. Enter your move (cell1-cell9):", null));

                String move = currentPlayer.receiveMessage();
                if (move == null || move.isEmpty()) {
                    handleDisconnection(currentPlayer, opponentPlayer);
                    break;
                }

                boolean moveSuccessful = processMove(move);
                if (!moveSuccessful) {
                    currentPlayer.sendMessage(new ResponsModel("error", "Invalid move. Try again.", null));
                    continue;
                }

                broadcastBoard();

                String gameState = gameModel.checkGameOver();
                if (gameState != null) {
                    handleGameOver(gameState);
                    break;
                }

                gameModel.setIsPlayerTurn(!gameModel.isPlayerTurn());
            }
        }  finally {
            cleanup();
            playerOne.endGame();
            playerTwo.endGame();
        }
    }

   

    private boolean processMove(String cellId) {
    System.out.println("[DEBUG] Processing move: Cell ID = " + cellId);

    if (!cellId.matches("cell[1-9]")) {
        System.out.println("[DEBUG] Invalid cell ID received: " + cellId);
        return false;
    }

    String currentSymbol = gameModel.isPlayerTurn() ? gameModel.getPlayer1Symbol() : gameModel.getPlayer2Symbol();
    boolean moveSuccessful = gameModel.makeMove(cellId, currentSymbol);

    if (!moveSuccessful) {
        System.out.println("[DEBUG] Move failed. Cell ID: " + cellId + ", Symbol: " + currentSymbol);
        System.out.println("[DEBUG] Current board state: " + Arrays.toString(gameModel.getBoard()));
    }

    return moveSuccessful;
}


    private void broadcastBoard() {
        StringBuilder boardState = new StringBuilder("Board state:\n");
        String[] board = gameModel.getBoard();
        ResponsModel boardResponse = new ResponsModel("update", "Board updated.", board);

        playerOne.sendMessage(boardResponse);
        playerTwo.sendMessage(boardResponse);

    }

    private void handleGameOver(String gameState) {
        ResponsModel gameOverResponse = new ResponsModel("gameOver", gameState, null);
        playerOne.sendMessage(gameOverResponse);
        playerTwo.sendMessage(gameOverResponse);
        isGameRunning = false;
    }
    
    private void handleDisconnection(ClientHandler disconnectedPlayer, ClientHandler opponentPlayer) {
        disconnectedPlayer.sendMessage(new ResponsModel("error", "You disconnected. Ending the game.", null));
        opponentPlayer.sendMessage(new ResponsModel("info", "Your opponent disconnected. You win!", null));
        isGameRunning = false;
    }

    private void cleanup() {
        try {
            playerOne.closeConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            playerTwo.closeConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Game thread ended. Connections closed.");
    }
}
