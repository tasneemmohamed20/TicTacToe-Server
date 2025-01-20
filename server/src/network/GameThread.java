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

    public GameThread(Socket player1Socket, Socket player2Socket, GameModel game, ClientHandler player1Handler, ClientHandler player2Handler) {
        this.player1Socket = player1Socket;
        this.player2Socket = player2Socket;
        this.game = game;
        this.player1Handler = player1Handler;
        this.player2Handler = player2Handler;

        try {
            // تهيئة تدفقات الإدخال والإخراج للاعبين
            player1Dis = new DataInputStream(player1Socket.getInputStream());
            player1Dos = new DataOutputStream(player1Socket.getOutputStream());
            player2Dis = new DataInputStream(player2Socket.getInputStream());
            player2Dos = new DataOutputStream(player2Socket.getOutputStream());
        } catch (IOException ex) {
            Logger.getLogger(GameThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        try {
            // إرسال رسالة بدء اللعبة إلى كلا اللاعبين
            sendToClient(player1Dos, new ResponsModel("start_game", "Game started. You are X.", game));
            sendToClient(player2Dos, new ResponsModel("start_game", "Game started. You are O.", game));
            System.out.println("Game started between " + game.getPlayer1() + " and " + game.getPlayer2());

            // حلقة اللعبة الرئيسية
            while (!game.isGameOver()) {
                try {
                    // استقبال حركة اللاعب 1
                    String player1Move = player1Dis.readUTF();
                    if (player1Move != null) {
                        handleMove(player1Move, game.getPlayer1());
                    }

                    // التحقق من انتهاء اللعبة بعد حركة اللاعب 1
                    if (game.isGameOver()) {
                        break;
                    }

                    // استقبال حركة اللاعب 2
                    String player2Move = player2Dis.readUTF();
                    if (player2Move != null) {
                        handleMove(player2Move, game.getPlayer2());
                    }
                } catch (IOException ex) {
                    Logger.getLogger(GameThread.class.getName()).log(Level.SEVERE, "Error reading move: " + ex.getMessage(), ex);
                    break;
                }
            }

            // إرسال رسالة نهاية اللعبة
            if (game.checkWinner(game.getPlayer1())) {
                sendToBothPlayers(new ResponsModel("game_over", "Player " + game.getPlayer1() + " wins!", game));
            } else if (game.checkWinner(game.getPlayer2())) {
                sendToBothPlayers(new ResponsModel("game_over", "Player " + game.getPlayer2() + " wins!", game));
            } else if (game.isDraw()) {
                sendToBothPlayers(new ResponsModel("game_over", "It's a draw!", game));
            }
        } catch (Exception ex) {
            Logger.getLogger(GameThread.class.getName()).log(Level.SEVERE, "Unexpected error: " + ex.getMessage(), ex);
        } finally {
            // تنظيف الموارد بعد انتهاء اللعبة
            closeResources();
        }
    }

    // معالجة حركة اللاعب
    private void handleMove(String move, String player) {
        try {
            // تحويل الحركة إلى رقم (موضع في اللوحة)
            int position = Integer.parseInt(move);

            // التحقق من صحة الحركة وتنفيذها
            if (position >= 0 && position < 9 && game.makeMove(player, position)) {
                // إرسال الحركة للاعب الآخر
                sendToBothPlayers(new ResponsModel("move", "Opponent's move: " + move, game));

                // التحقق من وجود فائز
                if (game.checkWinner(player)) {
                    sendToBothPlayers(new ResponsModel("game_over", "Player " + player + " wins!", game));
                } else if (game.isDraw()) {
                    sendToBothPlayers(new ResponsModel("game_over", "It's a draw!", game));
                }
            } else {
                // إرسال رسالة خطأ إذا كانت الحركة غير صالحة
                sendToClient(player.equals(game.getPlayer1()) ? player1Dos : player2Dos,
                        new ResponsModel("error", "Invalid move. Please try again.", null));
            }
        } catch (NumberFormatException ex) {
            // إرسال رسالة خطأ إذا كانت الحركة ليست رقمًا
            sendToClient(player.equals(game.getPlayer1()) ? player1Dos : player2Dos,
                    new ResponsModel("error", "Move must be a number.", null));
        } catch (Exception ex) {
            Logger.getLogger(GameThread.class.getName()).log(Level.SEVERE, "Error handling move: " + ex.getMessage(), ex);
        }
    }

    // إرسال رسالة إلى لاعب معين
    private void sendToClient(DataOutputStream dos, ResponsModel response) {
        try {
            String jsonResponse = gson.toJson(response);
            dos.writeUTF(jsonResponse);
            dos.flush();
        } catch (IOException ex) {
            Logger.getLogger(GameThread.class.getName()).log(Level.SEVERE, "Failed to send message to client.", ex);
        }
    }

    // إرسال رسالة إلى كلا اللاعبين
    private void sendToBothPlayers(ResponsModel response) {
        sendToClient(player1Dos, response);
        sendToClient(player2Dos, response);
    }

    // معالجة حركة الخصم
    public void handleOpponentMove(Object data) {
        Map<String, String> moveData = (Map<String, String>) data;
        String player = moveData.get("player");
        int position = Integer.parseInt(moveData.get("move"));

        // تحديث اللوحة
        updateBoard(position, player);

        // إرسال التحديث إلى اللاعب الآخر
        sendToBothPlayers(new ResponsModel("move", "Opponent's move: " + position, data));
    }

    // تحديث اللوحة بناءً على الحركة
    private void updateBoard(int position, String player) {
        // تحديث اللوحة في GameModel
        game.makeMove(player, position);

        // إرسال التحديث إلى اللاعبين
        sendToBothPlayers(new ResponsModel("move", "Opponent's move: " + position, game));
    }

    // تنظيف الموارد (إغلاق التدفقات والمقابس)
    private void closeResources() {
        try {
            if (player1Dis != null) player1Dis.close();
            if (player1Dos != null) player1Dos.close();
            if (player1Socket != null) player1Socket.close();
            if (player2Dis != null) player2Dis.close();
            if (player2Dos != null) player2Dos.close();
            if (player2Socket != null) player2Socket.close();
        } catch (IOException ex) {
            Logger.getLogger(GameThread.class.getName()).log(Level.SEVERE, "Error closing resources", ex);
        }
    }
}