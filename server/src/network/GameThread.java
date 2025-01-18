/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network;

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
            playerOne.sendMessage(new ResponsModel("info", "Game started! You are Player X.", null));
            playerTwo.sendMessage(new ResponsModel("info", "Game started! You are Player O.", null));

            while (isGameRunning) {
                ClientHandler currentPlayer = gameModel.isPlayerTurn() ? playerOne : playerTwo;
                ClientHandler opponentPlayer = gameModel.isPlayerTurn() ? playerTwo : playerOne;

                currentPlayer.sendMessage(new ResponsModel("info", "Your turn. Enter your move (cell1-cell9):", null));
                String move = currentPlayer.receiveMessage();

                if (move == null || move.isEmpty()) {
                    currentPlayer.sendMessage(new ResponsModel("info", "You disconnected. Ending the game.", null));
                    isGameRunning = false;
                    break;
                }

                if (processMove(move)) {
                    broadcastBoard();

                    String gameState = gameModel.checkGameState();
                    if (!gameState.equals("Ongoing")) {
                        playerOne.sendMessage(new ResponsModel("info", gameState, null));
                        playerTwo.sendMessage(new ResponsModel("info", gameState, null));
                        isGameRunning = false;
                    }
                } else {
                    currentPlayer.sendMessage(new ResponsModel("info", "Invalid move. Try again.", null));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private boolean processMove(String cellId) {
        String currentSymbol = gameModel.isPlayerTurn() ? gameModel.getPlayer1Symbol() : gameModel.getPlayer2Symbol();
        return gameModel.makeMove(cellId, currentSymbol);
    }

    private void broadcastBoard() {
        StringBuilder boardState = new StringBuilder("Board state:\n");
        String[] board = gameModel.getBoardState();

        for (int i = 0; i < board.length; i++) {
            boardState.append(board[i] == null ? "-" : board[i]);
            if ((i + 1) % 3 == 0) {
                boardState.append("\n");
            } else {
                boardState.append(" ");
            }
        }

        playerOne.sendMessage(new ResponsModel("info", boardState.toString(), null));
        playerTwo.sendMessage(new ResponsModel("info", boardState.toString(), null));
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
