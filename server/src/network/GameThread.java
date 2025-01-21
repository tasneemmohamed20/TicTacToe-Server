package network;

import com.google.gson.Gson;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import models.GameModel;
import models.ResponsModel;

public class GameThread extends Thread {

    private Socket player1Socket;
    private Socket player2Socket;
    private DataInputStream player1Dis;
    private DataOutputStream player1Dos;
    private DataInputStream player2Dis;
    private DataOutputStream player2Dos;
    private GameModel game;
    private Gson gson = new Gson();
    private ClientHandler player1Handler;
    private ClientHandler player2Handler;
    private String currentPlayer; // Current player's turn

    public GameThread(Socket player1Socket, Socket player2Socket, GameModel game, ClientHandler player1Handler, ClientHandler player2Handler) {
        this.player1Socket = player1Socket;
        this.player2Socket = player2Socket;
        this.game = game;
        this.player1Handler = player1Handler;
        this.player2Handler = player2Handler;
        this.currentPlayer = game.getPlayer1(); // Player 1 starts

        try {
            // Initialize input/output streams for players
            player1Dis = new DataInputStream(player1Socket.getInputStream());
            player1Dos = new DataOutputStream(player1Socket.getOutputStream());
            player2Dis = new DataInputStream(player2Socket.getInputStream());
            player2Dos = new DataOutputStream(player2Socket.getOutputStream());
        } catch (IOException ex) {

            Logger.getLogger(GameThread.class.getName()).log(Level.SEVERE, "Error initializing streams: ", ex);
        }
    }

    @Override
    public void run() {
        System.out.println("Game started between {} and {}" + game.getPlayer1() + game.getPlayer2());
        try {
              System.out.println("player1");
            // Send start game messages to both players
            sendToClient(player1Dos, new ResponsModel("start_game", "Game started. You are X.", game));

            sendToClient(player2Dos, new ResponsModel("start_game", "Game started. You are O.", game));
            

            // Main game loop
            while (!game.isGameOver()) {
                try {
                   // System.out.println("iiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiii");
                    // Receive move from the current player
                    
                        
                    if (player1Dis.available() > 0) {
                        System.out.println("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"+currentPlayer);
                        String move = player1Dis.readUTF();
                        System.out.println("ssssssssssssss" + move);
                         // handleMove(move, currentPlayer);
                    }
                    if (player2Dis.available() > 0) {
                        System.out.println("iii000000000000000000000000000000000000000000000000000iiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiii");
                    String move = player2Dis.readUTF();
                    System.out.println("ssssssssssssss" + move);
                   // handleMove(move, currentPlayer);
                    }
                   // System.err.println("move000000000000000000000000000000000");
                    /*String move = (currentPlayer.equals(game.getPlayer1())) ? player1Dis.readUTF() : player2Dis.readUTF();
                    System.out.println("Received move from {}: {}" + currentPlayer + ",move:" + move);
                    handleMove(move, currentPlayer);
*/
                    // Check if the game is over after each move
                  
                    if (game.isGameOver()) {
                        break;
                    }
                } catch (IOException ex) {
                    Logger.getLogger(GameThread.class.getName()).log(Level.SEVERE, "Error reading move: ", ex);
                    break;
                }
            }

            // Send game over message
            if (game.checkWinner(game.getPlayer1())) {
                sendToBothPlayers(new ResponsModel("game_over", "Player " + game.getPlayer1() + " wins!", game));
            } else if (game.checkWinner(game.getPlayer2())) {
                sendToBothPlayers(new ResponsModel("game_over", "Player " + game.getPlayer2() + " wins!", game));
            } else if (game.isDraw()) {
                sendToBothPlayers(new ResponsModel("game_over", "It's a draw!", game));
            }
        } catch (Exception ex) {
            Logger.getLogger(GameThread.class.getName()).log(Level.SEVERE, "Unexpected error: ", ex);
        } finally {
            // Clean up resources
            closeResources();
        }
    }

    // Handle a player's move
    private void handleMove(String move, String player) {
        try {
            int position = Integer.parseInt(move);

            // Validate and execute the move
            if (position >= 0 && position < 9 && game.makeMove(player, position)) {
                // Notify both players of the move
                sendToBothPlayers(new ResponsModel("move", "Opponent's move: " + move, game));

                // Check for a winner or draw
                if (game.checkWinner(player)) {
                    sendToBothPlayers(new ResponsModel("game_over", "Player " + player + " wins!", game));
                } else if (game.isDraw()) {
                    sendToBothPlayers(new ResponsModel("game_over", "It's a draw!", game));
                }

                // Switch turns
                currentPlayer = (currentPlayer.equals(game.getPlayer1())) ? game.getPlayer2() : game.getPlayer1();
            } else {
                // Notify the player of an invalid move
                sendToClient(player.equals(game.getPlayer1()) ? player1Dos : player2Dos,
                        new ResponsModel("error", "Invalid move. Please try again.", null));
            }
        } catch (NumberFormatException ex) {
            // Notify the player if the move is not a number
            sendToClient(player.equals(game.getPlayer1()) ? player1Dos : player2Dos,
                    new ResponsModel("error", "Move must be a number.", null));
        } catch (Exception ex) {
            Logger.getLogger(GameThread.class.getName()).log(Level.SEVERE, "Error handling move: ", ex);
        }
    }

    // Send a message to a specific client
    private void sendToClient(DataOutputStream dos, ResponsModel response) {
        try {
            String jsonResponse = gson.toJson(response);
            dos.writeUTF(jsonResponse);
            dos.flush();
        } catch (IOException ex) {
            Logger.getLogger(GameThread.class.getName()).log(Level.SEVERE, "Failed to send message to client: ", ex);
        }
    }

    // Send a message to both players
    private void sendToBothPlayers(ResponsModel response) {
        sendToClient(player1Dos, response);
        sendToClient(player2Dos, response);
    }

    // Handle opponent's move (called from ClientHandler)
    public void handleOpponentMove(Object data) {
        Map<String, String> moveData = (Map<String, String>) data;
        String player = moveData.get("player");
        int position = Integer.parseInt(moveData.get("move"));

        // Update the board
        game.makeMove(player, position);

        // Notify both players of the move
        sendToBothPlayers(new ResponsModel("move", "Opponent's move: " + position, game));
    }

    // Clean up resources
    private void closeResources() {
        try {
            if (player1Dis != null) {
                player1Dis.close();
            }
            if (player1Dos != null) {
                player1Dos.close();
            }
            if (player1Socket != null) {
                player1Socket.close();
            }
            if (player2Dis != null) {
                player2Dis.close();
            }
            if (player2Dos != null) {
                player2Dos.close();
            }
            if (player2Socket != null) {
                player2Socket.close();
            }
        } catch (IOException ex) {
            Logger.getLogger(GameThread.class.getName()).log(Level.SEVERE, "Error closing resources: ", ex);
        }
    }
}
