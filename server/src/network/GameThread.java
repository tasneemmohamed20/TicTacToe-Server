/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network;

import com.google.gson.Gson;
import db.DAO;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import models.GameModel;
import models.RequsetModel;
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
    private Gson gson = new Gson();

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
            try {
                new DAO().updateUserStatus(playerOne.name, "ingame");
                new DAO().updateUserStatus(playerTwo.name, "ingame");
            } catch (SQLException ex) {
                Logger.getLogger(GameThread.class.getName()).log(Level.SEVERE, null, ex);
            }

            broadcastBoard();
            while (isGameRunning) {
                ClientHandler currentPlayer = gameModel.isPlayerTurn() ? playerOne : playerTwo;
                ClientHandler opponentPlayer = gameModel.isPlayerTurn() ? playerTwo : playerOne;

                // currentPlayer.sendMessage(new ResponsModel("info", "Your turn. Enter your move (cell1-cell9):", null));
                String move = currentPlayer.receiveMessage();
                System.err.println("[DEBUG] Received move in GameThread: " + move);

                RequsetModel requestToUpdate = gson.fromJson(move, RequsetModel.class);
                if (requestToUpdate.getAction().equals("withdraw")) {
                    System.err.println("GameThread listening withdraw action");
                    handleQuitRequest(requestToUpdate);
                }
                if (requestToUpdate.getAction().equals("updateScore")) {
                    updateScore((Map<String, String>) requestToUpdate.getData());
                }

                if (requestToUpdate.getAction().equals("errorFromPlayer2")) {
                    sendServerErrorToP1();
                }

                if (move == null) {
                    System.out.println("[DEBUG] Player disconnected during their turn");
                    handleDisconnection(currentPlayer, opponentPlayer);
                    break;
                }

                if (move != null && move.startsWith("{")) {
                    RequsetModel request = gson.fromJson(move, RequsetModel.class);
                    if (request.getAction().equals("makeMove")) {
                        // Handle move
                        Map<String, String> moveData = (Map<String, String>) request.getData();
                        move = moveData.get("cell");
                    } else {
                        continue;
                    }
                }

                boolean moveSuccessful = processMove(move);
                if (!moveSuccessful) {
                    currentPlayer.sendMessage(new ResponsModel("error", "Invalid move. Try again.", null));
                    continue;
                }

                broadcastBoard();

                String gameState = gameModel.checkGameOver();
                if (!gameState.equals("Game ongoing")) {
                    handleGameOver(gameModel);
                    break;
                }

                // gameModel.setIsPlayerTurn(gameModel.isPlayerTurn());
            }
        } catch (RuntimeException e) {
            ClientHandler disconnectedPlayer = (e.getMessage() != null && e.getMessage().contains(playerOne.getName()))
                    ? playerOne : playerTwo;
            ClientHandler remainingPlayer = (disconnectedPlayer == playerOne) ? playerTwo : playerOne;
            handleDisconnection(playerTwo, playerOne);
        } finally {
            try {
                cleanup();
                new DAO().updateUserStatus(playerOne.name, "online");
                new DAO().updateUserStatus(playerTwo.name, "online");
                // playerOne.endGame();
                // playerTwo.endGame();
            } catch (SQLException ex) {
                Logger.getLogger(GameThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void handleQuitRequest(RequsetModel request) {
        Map<String, String> data = (Map<String, String>) request.getData();
        System.err.println("eeeeeeeeeeee" + data);
        String quittingPlayer = data.get("player");
        System.err.println("player" + quittingPlayer);

        ClientHandler opponent = (quittingPlayer.equals(playerOne.name)) ? playerTwo : playerOne;

        if (opponent != null) {
            System.err.println("[DEBUG] Sending withdraw message to opponent: " + opponent.name);

            opponent.sendMessage(new ResponsModel("withdraw", quittingPlayer + " withdrawing", null));
            try {
                boolean isUpdate = DAO.updateScoreByUsername(opponent.name);
                if (isUpdate) {
                    System.out.println("score " + opponent.name + " updated successfly. ==============================");
                } else {
                    System.out.println("something error!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                }
                new DAO().updateUserStatus(playerOne.name, "online");
                new DAO().updateUserStatus(playerTwo.name, "online");
            } catch (SQLException ex) {
                Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

        } else {
            System.err.println("[ERROR] Opponent not found or disconnected!");
        }

        //endGame();
    }

    private void endGame(String gameId) {
        isGameRunning = false;

        playerOne.endGame();
        playerTwo.endGame();

        // gamesList.remove(gameId);
    }

    private boolean processMove(String moveData) {
        System.out.println("[DEBUG] Processing move: Cell ID = " + moveData);

        if (!moveData.matches("cell[1-9]")) {
            System.out.println("[DEBUG] Invalid cell ID received: " + moveData);
            return false;
        }

        String currentSymbol = gameModel.isPlayerTurn() ? gameModel.getPlayer1Symbol() : gameModel.getPlayer2Symbol();
        System.out.println("[DEBUG] Current player: " + (gameModel.isPlayerTurn() ? "Player 1" : "Player 2"));
        System.out.println("[DEBUG] Current symbol: " + currentSymbol);

        boolean moveSuccessful = gameModel.makeMove(moveData, currentSymbol);

        if (!moveSuccessful) {
            System.out.println("[DEBUG] Move failed. Cell ID: " + moveData + ", Symbol: " + currentSymbol);
        }

        return moveSuccessful;
    }

    // private void broadcastBoard() {
    //     StringBuilder boardState = new StringBuilder("Board state:\n");
    //     String[] board = gameModel.getBoard();
    //     ResponsModel boardResponse = new ResponsModel("update", "Board updated.", board);
    //     playerOne.sendMessage(boardResponse);
    //     playerTwo.sendMessage(boardResponse);
    // }
    private void broadcastBoard() {
        String[] board = gameModel.getBoard();
        String currentTurn = gameModel.getCurrentPlayer().equals(gameModel.getPlayer1())
                ? gameModel.getPlayer1Symbol()
                : gameModel.getPlayer2Symbol();
        // String currentTurn = !gameModel.isPlayerTurn() ? gameModel.getPlayer1Symbol() : gameModel.getPlayer2Symbol();
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("board", board);
        updateData.put("currentTurn", currentTurn);
        updateData.put("currentPlayer", gameModel.getCurrentPlayer());

        System.out.println("[DEBUG] Broadcasting board state: " + Arrays.toString(board));
        System.out.println("[DEBUG] Current turn: " + currentTurn);
        System.out.println("[DEBUG] Current turn: " + currentTurn);

        ResponsModel boardResponse = new ResponsModel("update", "Board updated.", updateData);
        playerOne.sendMessage(boardResponse);
        playerTwo.sendMessage(boardResponse);

        ClientHandler currentPlayer = gameModel.getCurrentPlayer().equals(gameModel.getPlayer1()) ? playerOne : playerTwo;
        currentPlayer.sendMessage(new ResponsModel("info", "Your turn. Enter your move (cell1-cell9):", null));

    }

    private void handleGameOver(GameModel gameModel) {
        /*
        Map<String, String> data = new HashMap<>();
        data.put("player1", playerOne.name);
        data.put("player2", playerTwo.name);
        ResponsModel gameOverResponse = new ResponsModel("gameOver", gameModel.checkGameOver(), data);
        playerOne.sendMessage(gameOverResponse);
        playerTwo.sendMessage(gameOverResponse);
        isGameRunning = false;
         */

        Map<String, Object> data = new HashMap<>();
        data.put("player1", playerOne.name);
        data.put("player2", playerTwo.name);
        data.put("winningLine", gameModel.getWinningLine());

        System.out.println("Sending gameOver response with winning line: " + Arrays.toString(gameModel.getWinningLine()));
        ResponsModel gameOverResponse = new ResponsModel("gameOver", gameModel.checkGameOver(), data);
        playerOne.sendMessage(gameOverResponse);
        playerTwo.sendMessage(gameOverResponse);
        isGameRunning = false;
    }

    private void handleDisconnection(ClientHandler disconnectedPlayer, ClientHandler remainingPlayer) {
        try {
            // Notify remaining player of win
            remainingPlayer.sendMessage(new ResponsModel("info", "Your opponent disconnected. You win!", null));
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to notify remaining player: " + e.getMessage());
        }
        isGameRunning = false;
    }

    private void cleanup() {
        // try {
        //     playerOne.closeConnection();
        // } catch (Exception e) {
        //     e.printStackTrace();
        // }
        // try {
        //     playerTwo.closeConnection();
        // } catch (Exception e) {
        //     e.printStackTrace();
        // }

        playerOne.endGame();
        playerTwo.endGame();
        isGameRunning = false;
        System.out.println("Game thread ended. Connections closed.");
    }

    private void updateScore(Map<String, String> data) {
        String name = data.get("name");
        try {
            boolean isUpdate = DAO.updateScoreByUsername(name);
            if (isUpdate) {
                System.out.println("score " + name + " updated successfly. ==============================");
            } else {
                System.out.println("something error!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            }
        } catch (SQLException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }

        
    }

    private void sendServerErrorToP1() {
        ResponsModel boardResponse = new ResponsModel("errorFromPlayer2", "Server Stoped !!", null);
        playerOne.sendMessage(boardResponse);
        System.out.println("sendServerErrorToP1 ===============================");
    }
}
