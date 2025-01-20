package models;

public class GameModel {
    private String player1; // اللاعب الأول (X)
    private String player2; // اللاعب الثاني (O)
    private String[][] board; // لوحة اللعبة (3x3)
    private String currentPlayer; // اللاعب الحالي
    private boolean gameOver; // حالة اللعبة (منتهية أو لا)

    public GameModel(String player1, String player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.board = new String[3][3]; // لوحة 3x3
        this.currentPlayer = player1; // اللاعب الأول يبدأ
        this.gameOver = false; // اللعبة لم تنتهِ بعد
    }

    
    public boolean makeMove(String player, int position) {
        int row = position / 3; // حساب الصف
        int col = position % 3; // حساب العمود

        // التحقق إذا كانت الخلية فارغة
        if (board[row][col] == null || board[row][col].isEmpty()) {
            board[row][col] = player; // تعيين الحركة
            currentPlayer = (player.equals(player1)) ? player2 : player1; // تغيير الدور
            return true; // الحركة صالحة
        }
        return false; // الحركة غير صالحة (الخلية محجوزة بالفعل)
    }

    public boolean checkWinner(String player) {
        // التحقق من الصفوف
        for (int i = 0; i < 3; i++) {
            if (board[i][0] != null && board[i][0].equals(player) &&
                board[i][1] != null && board[i][1].equals(player) &&
                board[i][2] != null && board[i][2].equals(player)) {
                gameOver = true; // اللعبة انتهت
                return true; // فوز في الصف
            }
        }

        // التحقق من الأعمدة
        for (int j = 0; j < 3; j++) {
            if (board[0][j] != null && board[0][j].equals(player) &&
                board[1][j] != null && board[1][j].equals(player) &&
                board[2][j] != null && board[2][j].equals(player)) {
                gameOver = true; // اللعبة انتهت
                return true; // فوز في العمود
            }
        }

        // التحقق من القطر الرئيسي (من اليسار لليمين)
        if (board[0][0] != null && board[0][0].equals(player) &&
            board[1][1] != null && board[1][1].equals(player) &&
            board[2][2] != null && board[2][2].equals(player)) {
            gameOver = true; // اللعبة انتهت
            return true; // فوز في القطر الرئيسي
        }

       
        if (board[0][2] != null && board[0][2].equals(player) &&
            board[1][1] != null && board[1][1].equals(player) &&
            board[2][0] != null && board[2][0].equals(player)) {
            gameOver = true; // اللعبة انتهت
            return true; 
        }

        return false; 
    }

    
    public boolean isDraw() {
      
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == null || board[i][j].isEmpty()) {
                    return false;
                }
            }
        }
        gameOver = true; 
        return true; 
    }

   
    public String getPlayer1() {
        return player1;
    }

    public String getPlayer2() {
        return player2;
    }

  
    public String getCurrentPlayer() {
        return currentPlayer;
    }

 
    public String[][] getBoard() {
        return board;
    }

    public boolean isGameOver() {
        return gameOver;
    }


    public String getWinner() {
        if (checkWinner(player1)) {
            return player1;
        } else if (checkWinner(player2)) {
            return player2;
        } else if (isDraw()) {
            return "Draw";
        }
        return null; 
    }
}