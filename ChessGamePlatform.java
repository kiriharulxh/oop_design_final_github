import java.util.*;
import java.io.*;
import java.math.*;

abstract class MementoIF implements Serializable {}

class Memento extends MementoIF {

    protected String gameType; // 游戏类型，用于确定读档正确性

    protected int chessboardLength; // 棋盘大小(8 <= length <= 19)
    protected String board; // 棋盘
    protected String history_1; // 历史(用于悔棋，仅限一步)
    protected String history_2; // 历史(用于悔棋，仅限一步)
    // protected boolean canRegret = false;
    protected char currentPlayer; // 当前玩家('B': 黑棋, 'W': 白棋)

    protected HashMap<Character, Boolean> canRegret;

    public void setStatus(int boardlen, String bod, String hist1, String hist2, HashMap<Character, Boolean> regret, char curplayer) {
        this.chessboardLength = boardlen;
        this.board = bod;
        this.history_1 = hist1;
        this.history_2 = hist2;

        this.canRegret = regret;
        this.currentPlayer = curplayer;
    }

    public int getchessboardLength() { return this.chessboardLength; }
    public String getBoard() { return this.board; }
    public String getHistory_1() { return this.history_1; }
    public String getHistory_2() { return this.history_2; }

    public HashMap<Character, Boolean> getCanRegret() { return this.canRegret; }
    public char getCurrentPlayer() { return this.currentPlayer; }
    public String getGameType() { return this.gameType; }

}

class WuZiMemento extends Memento {

    WuZiMemento() {
        super();
        this.gameType = "Wuzi";
    }

    public void setStatus(int boardlen, String bod, String hist1, String hist2, HashMap<Character, Boolean> regret, char curplayer, String gType) {
        assert gType.equalsIgnoreCase("WuZi");

        super.setStatus(boardlen, bod, hist1, hist2, regret, curplayer);
    }

}

class GoMemento extends Memento {
    private boolean passOnce;
    private String passPlayer;

    GoMemento() {
        super();
        this.gameType = "Go";
    }

    public void setStatus(int boardlen, String bod, String hist1, String hist2, HashMap<Character, Boolean> regret, char curplayer, boolean poc, String passPl, String gType) {
        assert gType.equalsIgnoreCase("Go");
        super.setStatus(boardlen, bod, hist1, hist2, regret, curplayer);
        this.passOnce = poc;
        this.passPlayer = passPl;
    }

    public boolean getPassOnce() { return passOnce; }
    public String getPassPlayer() { return passPlayer; }

}

class ChessBoardManager { // Caretaker
    private ChessGame game;
    private MementoIF memento;

    public void save(String path) {
        // long timestamp = new Date().getTime();
        try {
            FileOutputStream fos = new FileOutputStream(path);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(this.memento);

            oos.close();
            fos.close();
            System.out.println("Memento written to " + path + " !");
        } catch (FileNotFoundException e) {
            System.out.println(e);
        } catch (IOException e) {
            System.out.println(e);
        }

    }

    public void load(String path) {
        try {
            FileInputStream fis = new FileInputStream(path);
            ObjectInputStream ois = new ObjectInputStream(fis);
            this.memento = (MementoIF) ois.readObject();

            ois.close();
            fis.close();
            System.out.println("Memento read from " + path + " !\n");
        } catch (FileNotFoundException e) {
            System.out.println(e);
        } catch (IOException e) {
            System.out.println(e);
        } catch (ClassNotFoundException e) {
            System.out.println(e);
        }
    }
    
    public void setGame(ChessGame g) { this.game = g; }

    public void createMemo() {
        this.memento = this.game.save();
    }

    public MementoIF getMemento() {return this.memento;}

}


class ChessGame { // Originator

    protected int chessboardLength; // 棋盘大小(8 <= length <= 19)
    protected char[][] board; // 棋盘
    protected char[][] history_1; // 历史(用于悔棋，仅限一步)
    protected char[][] history_2; // 历史(用于悔棋，仅限一步)

    protected HashMap<Character, Boolean> canRegret = new HashMap<Character, Boolean>();
    protected char currentPlayer; // 当前玩家('B': 黑棋, 'W': 白棋)

    public boolean validPlay(int x, int y) {
        try {
            if (this.board[x][y] != '-') {
                return false;
            }
        } catch (Exception e) {
            System.out.println(e);
            return false;
        }
        return true;
    }

    public void restart() {
        initChessBoard();
        this.history_1 = null;
        this.history_2 = null;
        this.canRegret.put('W', true);
        this.canRegret.put('B', true);
    }

    public boolean checkPlayable() {
        for (int i = 0; i < this.chessboardLength; i++) {
            for (int j = 0; j < this.chessboardLength; j++) {
                if (this.board[i][j] == '-') return true;
            }
        }
        return false;
    }

    public boolean makeMove(int x, int y) { 
        if (!validPlay(x, y)) return false;

        maintainMemory();
        changeBoard(x, y);

        return true;
    }

    public void maintainMemory() {
        if (this.history_1 != null) {
            if (this.history_2 == null) {
                this.history_2 = new char[chessboardLength][chessboardLength];
            }
            setHistory2(this.history_1);
        }
        if (this.history_1 == null) this.history_1 = new char[chessboardLength][chessboardLength];
        setHistory1(this.board);

    }

    public void switchPlayer() {
        currentPlayer = currentPlayer == 'B' ? 'W' : 'B'; // 切换玩家
    }

    public void changeBoard(int x, int y) {
        this.board[x][y] = currentPlayer;
    }

    public boolean regret() {
        if(canRegret() && (history_2 != null)) {
            setChessBoard(history_2);
            history_1 = null;
            history_2 = null;
            setRegretState(false);
            return true;
        }
        return false;
    }

    public String arr2str(char[][] arr) {
        String result = new String("");
        if (arr != null) {
            for (int i = 0; i < chessboardLength; i++) {
                result = result + new String(arr[i]);
            }
        }
        return result;
    }

    public MementoIF save() {
        Memento memento = new Memento();
        memento.setStatus(chessboardLength, arr2str(board), arr2str(history_1), arr2str(history_2), canRegret, currentPlayer);
        return memento;
    }

    public void setRegretState(boolean state) { this.canRegret.put(currentPlayer, state); }
    public boolean canRegret() { 
        return canRegret.get(currentPlayer); 
    }

    public void printMessage(String message) {
        System.out.println(message);
    }

    ChessGame(int cbl, char cpl) {
        this.chessboardLength = cbl;
        this.currentPlayer = cpl;
        this.canRegret.put('W', true);
        this.canRegret.put('B', true);
        initChessBoard();

    }

    ChessGame(MementoIF memo) {
        Memento memento = (Memento) memo;

        this.chessboardLength = memento.getchessboardLength();
        this.initChessBoardAndHistory(memento.getBoard(), memento.getHistory_1(), memento.getHistory_2());

        this.canRegret = memento.getCanRegret();
        this.currentPlayer = memento.getCurrentPlayer();
    }


    public void initChessBoard(){
        this.board = new char[chessboardLength][chessboardLength];
        for (int i = 0; i < chessboardLength; i++) {
            for (int j = 0; j < chessboardLength; j++) {
                this.board[i][j] = '-';
            }
        }
        
    }

    public void printBoard() {
        System.out.print("   ");
        for (int j = 0; j < chessboardLength; j++) {
            System.out.print(j + " ");
            if (j < 9) System.out.print(" ");
        }
        System.out.println();
        for (int i = 0; i < chessboardLength; i++) {
            System.out.print((char)((int)('A') + i) + "  ");

            for (int j = 0; j < chessboardLength; j++) {
                System.out.print(this.board[i][j] + "  ");
            }
            System.out.println();
        }
    }

    public void setChessBoard(char [][] other_board){
        for (int i = 0; i < chessboardLength; i++) {
            for (int j = 0; j < chessboardLength; j++) {
                board[i][j] = other_board[i][j];
            }
        }
    }

    public void setHistory1(char [][] other_board){
        for (int i = 0; i < chessboardLength; i++) {
            for (int j = 0; j < chessboardLength; j++) {
                history_1[i][j] = other_board[i][j];
            }
        }
    }

    public void setHistory2(char [][] other_board){
        for (int i = 0; i < chessboardLength; i++) {
            for (int j = 0; j < chessboardLength; j++) {
                history_2[i][j] = other_board[i][j];
            }
        }
    }

    public void initChessBoardAndHistory(String other_board, String other_history_1, String other_history_2){
        board = new char[chessboardLength][chessboardLength];
        if (other_history_1.equalsIgnoreCase("")) history_1 = null;
        else history_1 = new char[chessboardLength][chessboardLength];

        if (other_history_2.equalsIgnoreCase("")) history_2 = null;
        else history_2 = new char[chessboardLength][chessboardLength];
        
        for (int i = 0; i < chessboardLength; i++) {
            for (int j = 0; j < chessboardLength; j++) {
                board[i][j] = other_board.charAt(i*chessboardLength + j);
                if (!other_history_1.equalsIgnoreCase("")) {
                    history_1[i][j] = other_history_1.toCharArray()[i*chessboardLength + j];
                    // System.out.println(other_history_1);
                }
                if (!other_history_2.equalsIgnoreCase("")) {
                    history_2[i][j] = other_history_2.toCharArray()[i*chessboardLength + j];
                    // System.out.println(other_history_2);
                }
            }
        }
    }

    public char getCurrentPlayer(){ return currentPlayer; }

}

class WuZi extends ChessGame {

    WuZi(int cbl) {
        super(cbl, 'W'); // 白棋先行
    }

    WuZi(MementoIF memo) {
        super(memo);
    }
    
    @Override
    public MementoIF save() {
        WuZiMemento memento = new WuZiMemento();
        memento.setStatus(chessboardLength, arr2str(board), arr2str(history_1), arr2str(history_2), canRegret, currentPlayer, "WuZi");
        return memento;
    }

    @Override
    public void restart(){
        super.restart();
        currentPlayer = 'W';
    }

    public boolean checkFinish(int x, int y) {
        for (int dx=-1; dx<=1; dx++) {
            for (int dy=-1; dy<=0; dy++) {
                if (dx == 0 && dy == 0) continue;
                int count = 0;
                int nx = x;
                int ny = y;
                while (true) {
                    nx += dx;
                    ny += dy;
                    if (nx < 0 || nx >= chessboardLength || ny < 0 || ny >= chessboardLength) break;
                    if (board[nx][ny] != currentPlayer) break;
                }
                while (true) {
                    nx -= dx;
                    ny -= dy;
                    if (nx < 0 || nx >= chessboardLength || ny < 0 || ny >= chessboardLength) break;
                    if (board[nx][ny] != currentPlayer) break;
                    count ++;
                    if (count >= 5) return true;
                }
                
            }
        }
        return  false;
    }

}

class GoGame extends ChessGame {
    private boolean passOnce = false;
    private String passPlayer = "";

    GoGame(int cbl) {
        super(cbl, 'B'); // 黑棋先行
    }

    GoGame(MementoIF memo) {
        super(memo);
        GoMemento goMemento = (GoMemento) memo;
        this.setPassOnce(goMemento.getPassOnce());
        this.setPassPlayer(goMemento.getPassPlayer());
    }

    public void setPassOnce(boolean poc) { this.passOnce = poc; }
    public boolean getPassOnce() { return passOnce; }

    public void setPassPlayer(String name) { this.passPlayer = name; }
    public String getPassPlayer() { return this.passPlayer; }


    @Override
    public MementoIF save() {
        GoMemento memento = new GoMemento();
        memento.setStatus(chessboardLength, arr2str(board), arr2str(history_1), arr2str(history_2), canRegret, currentPlayer, passOnce, passPlayer, "Go");
        return memento;
    }

    @Override
    public void restart(){
        super.restart();
        currentPlayer = 'B';
    }

    @Override
    public boolean validPlay(int x, int y) {
        if (super.validPlay(x, y)) {
            this.board[x][y] = currentPlayer;
            if (findQi(x, y, currentPlayer)) { // 未考虑打劫
                this.board[x][y] = '-';
                return true;
            }
            this.board[x][y] = '-';
        }
        return false;
    }

    @Override
    public void changeBoard(int x, int y) {
        super.changeBoard(x, y);
        eatDeadChess(x, y);
    }

    @Override
    public boolean makeMove(int x, int y) { 
        if (!this.validPlay(x, y)) return false;

        maintainMemory();
        this.changeBoard(x, y);

        return true;
    }

    @Override
    public boolean checkPlayable() {
        for (int i = 0; i < chessboardLength; i++) {
            for (int j = 0; j < chessboardLength; j++) {
                if (this.board[i][j] == '-') {
                    this.board[i][j] = currentPlayer;
                    if (findQi(i, j, currentPlayer)) {
                        this.board[i][j] = '-';
                        return true;
                    }
                    this.board[i][j] = '-';
                }
            }
        }
        return false;
    }

    public void eatChess(int x, int y, char player) {
        if (findQi(x, y, player)) return;

        ArrayDeque<Integer> pos_x = new ArrayDeque<Integer>();
        ArrayDeque<Integer> pos_y = new ArrayDeque<Integer>();
        
        pos_x.add(x);
        pos_y.add(y);

        while(!pos_x.isEmpty()) {
            int posX = pos_x.removeFirst();
            int posY = pos_y.removeFirst();

            board[posX][posY] = '-';
            if (posX != 0) { 
                if (board[posX-1][posY] == player) {
                    pos_x.addLast(posX-1);
                    pos_y.addLast(posY);
                }
            }
            if (posX != (chessboardLength-1)) {
                if (board[posX+1][posY] == player) {
                    pos_x.addLast(posX+1);
                    pos_y.addLast(posY);
                }
            }
            if (posY != 0) {
                if (board[posX][posY-1] == player) {
                    pos_x.addLast(posX);
                    pos_y.addLast(posY-1);
                }
            }
            if (posY != (chessboardLength-1)) {
                if (board[posX][posY+1] == player) {
                    pos_x.addLast(posX);
                    pos_y.addLast(posY+1);
                }
            }

        }

    }

    public boolean findQi(int x, int y, char player) {
        ArrayDeque<Integer> pos_x = new ArrayDeque<Integer>();
        ArrayDeque<Integer> pos_y = new ArrayDeque<Integer>();

        HashSet<String> registered = new HashSet<String>();
        
        pos_x.add(x);
        pos_y.add(y);

        while(!pos_x.isEmpty()) {
            int posX = pos_x.removeFirst();
            int posY = pos_y.removeFirst();
            String reg_id = new String(Integer.toString(posX) + "," + Integer.toString(posY));
            if (registered.contains(reg_id)) continue;

            if (posX != 0) {
                if (board[posX-1][posY] == '-') return true;
                if (board[posX-1][posY] == player) {
                    pos_x.addLast(posX-1);
                    pos_y.addLast(posY);
                }
            }
            if (posX != (chessboardLength-1)) {
                if (board[posX+1][posY] == '-') return true;
                if (board[posX+1][posY] == player) {
                    pos_x.addLast(posX+1);
                    pos_y.addLast(posY);
                }
            }
            if (posY != 0) {
                if (board[posX][posY-1] == '-') return true;
                if (board[posX][posY-1] == player) {
                    pos_x.addLast(posX);
                    pos_y.addLast(posY-1);
                }
            }
            if (posY != (chessboardLength-1)) {
                if (board[posX][posY+1] == '-') return true;
                if (board[posX][posY+1] == player) {
                    pos_x.addLast(posX);
                    pos_y.addLast(posY+1);
                }
            }

            registered.add(reg_id);
        }

        return false;
    }

    public void eatDeadChess(int x, int y) {
        char checkPlayer = currentPlayer == 'B' ? 'W' : 'B';

        if (x != 0){
            if (board[x-1][y] == checkPlayer) eatChess(x-1, y, checkPlayer);
        }
        if (x != (chessboardLength-1)) {
            if (board[x+1][y] == checkPlayer) eatChess(x+1, y, checkPlayer);
        }
        if (y != 0){
            if (board[x][y-1] == checkPlayer) eatChess(x, y-1, checkPlayer);
        }
        if (y != (chessboardLength-1)) {
            if (board[x][y+1] == checkPlayer) eatChess(x, y+1, checkPlayer);
        }
    }

    public String checkWinner() {
        int cnt_w = 0;
        int cnt_b = 0;

        for (int i = 0; i < chessboardLength; i++) {
            for (int j = 0; j < chessboardLength; j++) {
                if (board[i][j] == 'W') {
                    cnt_w += 1;
                } else if (board[i][j] == 'B') {
                    cnt_b += 1;
                }
            }
        }
        return (cnt_b > cnt_w) ? "黑棋" : "白棋";
    }

}


class ReversiMemento extends Memento {
    ReversiMemento() {
        super();
        this.gameType = "Reversi";
    }

    public void setStatus(String bod, String hist1, String hist2, HashMap<Character, Boolean> regret, char curplayer, String gType) {
        assert gType.equalsIgnoreCase("Go");
        super.setStatus(8, bod, hist1, hist2, regret, curplayer);
    }

}

class Reversi extends ChessGame {

    private int[][] directions = {{-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}};
    private char[][] board_backup = new char[8][8];

    Reversi() {
        super(8, 'B');
        this.board[3][3] = 'B';
        this.board[3][4] = 'W';
        this.board[4][4] = 'B';
        this.board[4][3] = 'W';
    }

    Reversi(MementoIF memo) {
        super(memo);
        ReversiMemento reversiMemento = (ReversiMemento) memo;
        this.board[3][3] = 'B';
        this.board[3][4] = 'W';
        this.board[4][4] = 'B';
        this.board[4][3] = 'W';

    }

    @Override
    public MementoIF save() {
        ReversiMemento memento = new ReversiMemento();
        memento.setStatus(arr2str(board), arr2str(history_1), arr2str(history_2), canRegret, currentPlayer, "Reversi");
        return memento;
    }

    @Override
    public void restart(){
        super.restart();
        currentPlayer = 'B';
        this.board[3][3] = 'X';
        this.board[3][4] = 'O';
        this.board[4][4] = 'X';
        this.board[4][3] = 'O';
    }

    @Override
    public boolean validPlay(int x, int y) {
        if (super.validPlay(x, y)) {
            for (int[] direction : directions) {
                if (canMove(x, y, direction, currentPlayer)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean inBoard(int i, int j) {
        return i >= 0 && i < 8 && j >= 0 && j < 8;
    }

    public boolean canMove(int i, int j, int[] direction, char player) {
        i += direction[0];
        j += direction[1];
        if (!inBoard(i, j) || this.board[i][j] == '-' || this.board[i][j] == player) return false;
        while (this.board[i][j] != player) {
            i += direction[0];
            j += direction[1];
            if (!inBoard(i, j) || this.board[i][j] == '-') return false;
        }
        return true;
    }

    @Override
    public void changeBoard(int i, int j) {
        super.changeBoard(i, j);

        for (int[] direction : directions) {
            if (!canMove(i, j, direction, currentPlayer)) continue;
            int x = i + direction[0];
            int y = j + direction[1];
            while (board[x][y] != currentPlayer) {
                board[x][y] = currentPlayer;
                x += direction[0];
                y += direction[1];
            }
        }
    }

    @Override
    public boolean makeMove(int x, int y) { 
        if (!this.validPlay(x, y)) return false;

        maintainMemory();
        this.changeBoard(x, y);

        return true;
    }

    public void tryMove(int x, int y) {
        for (int i = 0; i < this.chessboardLength; i++) {
            for (int j = 0; j < this.chessboardLength; j++) {
                this.board_backup[i][j] = this.board[i][j];
            }
        }
        this.changeBoard(x, y);
    }

    public void restoreBoard() {
        for (int i = 0; i < this.chessboardLength; i++) {
            for (int j = 0; j < this.chessboardLength; j++) {
                this.board[i][j] = this.board_backup[i][j];
            }
        }
    }

    @Override
    public boolean checkPlayable() {
        for (int i = 0; i < chessboardLength; i++) {
            for (int j = 0; j < chessboardLength; j++) {
                if (board[i][j] == '-') {
                    if (validPlay(i, j)) return true;
                }
            }
        }
        return false;
    }

    public int countRivalPossibleSteps(char player) {
        int steps = 0;
        assert player != currentPlayer;
        this.switchPlayer();
        for (int i = 0; i < chessboardLength; i++) {
            for (int j = 0; j < chessboardLength; j++) {
                if (board[i][j] == '-') {
                    if (validPlay(i, j)) steps ++;
                }
            }
        }
        this.switchPlayer();
        return steps;
    }

    public String checkWinner() {
        int cnt_w = 0;
        int cnt_b = 0;

        for (int i = 0; i < chessboardLength; i++) {
            for (int j = 0; j < chessboardLength; j++) {
                if (board[i][j] == 'W') {
                    cnt_w += 1;
                } else if (board[i][j] == 'B') {
                    cnt_b += 1;
                }
            }
        }
        if (cnt_b == cnt_w) return "平局";
        return (cnt_b > cnt_w) ? "黑棋" : "白棋";
    }

}


interface TerminalState {
    void interfaceLogic(UserTerminal terminal); 
}


class SystemModeState implements TerminalState {

    private static final SystemModeState instance = new SystemModeState();
    private SystemModeState() {}
    public static SystemModeState getInstance() { return instance; }

    public void interfaceLogic(UserTerminal terminal) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("1.开始游戏\n2.查看回放\n");
        String input_gtype = scanner.nextLine();
        if (input_gtype.equals("1")) {
            terminal.setState(LoginState.getInstance());
        } else if (input_gtype.equals("2")) {
            terminal.setState(RecordState.getInstance());
        } else {
            System.out.println("非法输入！");
            return;
        }
    }

}


class RecordState implements TerminalState {

    private static final RecordState instance = new RecordState();
    private RecordState() {}
    public static RecordState getInstance() { return instance; }

    public void interfaceLogic(UserTerminal terminal) {
        Vector<String> gameRecord = new Vector<String>();
        Scanner scanner = new Scanner(System.in);
        System.out.println("输入要读取的回放文件名：");
        String input_gtype = scanner.nextLine();
        try {
            FileInputStream record_fis = new FileInputStream(input_gtype);
            ObjectInputStream record_ois = new ObjectInputStream(record_fis);
            gameRecord = (Vector<String>) record_ois.readObject();

            record_ois.close();
            record_fis.close();

        } catch (FileNotFoundException e) {
            System.out.println(e);
        } catch (IOException e) {
            System.out.println(e);
        } catch (ClassNotFoundException e) {
            System.out.println(e);
        }

        System.out.println("读取成功");
        int step = 0;
        int max_step = gameRecord.size();
        int cbl = Integer.valueOf(gameRecord.get(max_step - 1));
        String username1 = gameRecord.get(max_step - 3);
        String username2 = gameRecord.get(max_step - 2);
        System.out.println("回放开始");
        String board = gameRecord.get(step);
        printBoardStr(board, cbl);
        terminal.printUserInfo(username1, username2);
        System.out.println("当前为：第" + (step + 1) + "步");

        while (step < max_step - 3) {
            if (step < max_step - 4) System.out.print("输入1进入下一步；");
            if (step > 0) System.out.print("输入2进入上一步；");
            System.out.println("输入q退出回放");

            String input = scanner.nextLine();
            if (input.equals("1") && step < max_step - 4) {
                step++;
                board = gameRecord.get(step);
                printBoardStr(board, cbl);
                terminal.printUserInfo(username1, username2);
                System.out.println("当前为：第" + (step + 1) + "步");
                continue;
            }
            if (input.equals("2") && step > 0) {
                step--;
                board = gameRecord.get(step);
                printBoardStr(board, cbl);
                terminal.printUserInfo(username1, username2);
                System.out.println("当前为：第" + (step + 1) + "步");
                continue;
            }
            if (input.equals("q")) {
                break;
            }
            else {
                System.out.println("输入错误，请重新输入");
                continue;
            }

        }

        terminal.setState(SystemModeState.getInstance());
    }

    public void printBoardStr(String board, int chessboardLength) {
        System.out.print("   ");
        for (int j = 0; j < chessboardLength; j++) {
            System.out.print(j + " ");
            if (j < 9) System.out.print(" ");
        }
        System.out.println();
        for (int i = 0; i < chessboardLength; i++) {
            System.out.print((char)((int)('A') + i) + "  ");

            for (int j = 0; j < chessboardLength; j++) {
                System.out.print(board.toCharArray()[i*chessboardLength + j] + "  ");
            }
            System.out.println();
        }
    }

}


class LoginState implements TerminalState {

    private static final LoginState instance = new LoginState();
    private LoginState() {}
    public static LoginState getInstance() { return instance; }

    public void interfaceLogic(UserTerminal terminal) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("选择玩家1(白棋)类型：1为AI，2为用户，3为游客, 4为二级AI（仅支持黑白棋，其他模式下自动降为普通AI）");
        String input_gtype = scanner.nextLine();
        if (!input_gtype.equalsIgnoreCase("1") && !input_gtype.equalsIgnoreCase("2") && !input_gtype.equalsIgnoreCase("3") && !input_gtype.equalsIgnoreCase("4")) {
            System.out.println("非法输入！");
            return;
        }

        if (input_gtype.equalsIgnoreCase("1")) {
            terminal.setUser1(new UserInfo("AI"));
        } else if (input_gtype.equalsIgnoreCase("2")) {
            System.out.println("请输入玩家1用户名：");
            String input_uname = scanner.nextLine();
            System.out.println("请输入玩家1密码：");
            String input_pwd = scanner.nextLine();
            if (terminal.userKeyMap.containsKey(input_uname) && UserInfo.checkUserInfo(input_uname, input_pwd, terminal.userKeyMap)) {
                terminal.setUser1(terminal.userInfoMap.get(input_uname));
            }
            else if (terminal.userKeyMap.containsKey(input_uname)) {
                System.out.println("密码错误！");
                return;
            }
            else {
                System.out.println("用户不存在，为您自动注册");
                terminal.setUser1(new UserInfo(input_uname));
                String pwd = input_uname + input_pwd;
                terminal.userKeyMap.put(input_uname, pwd.hashCode());
                terminal.userInfoMap.put(input_uname, terminal.getUser1());
            }
        } else if (input_gtype.equalsIgnoreCase("3")) {
            terminal.setUser1(new UserInfo("游客"));
        } else if (input_gtype.equalsIgnoreCase("4")) {
            terminal.setUser1(new UserInfo("二级AI"));
        }

        System.out.println("选择玩家2（黑棋）类型：1为AI，2为用户，3为游客, 4为二级AI（仅支持黑白棋，其他模式下自动降为普通AI）");
        input_gtype = scanner.nextLine();
        if (!input_gtype.equalsIgnoreCase("1") && !input_gtype.equalsIgnoreCase("2") && !input_gtype.equalsIgnoreCase("3") && !input_gtype.equalsIgnoreCase("4")) {
            System.out.println("非法输入！");
            return;
        }

        if (input_gtype.equalsIgnoreCase("1")) {
            terminal.setUser2(new UserInfo("AI"));

        } else if (input_gtype.equalsIgnoreCase("2")) {
            System.out.println("请输入玩家2用户名：");
            String input_uname = scanner.nextLine();
            System.out.println("请输入玩家2密码：");
            String input_pwd = scanner.nextLine();
            if (terminal.userKeyMap.containsKey(input_uname) && UserInfo.checkUserInfo(input_uname, input_pwd, terminal.userKeyMap)) {
                terminal.setUser2(terminal.userInfoMap.get(input_uname));
            }
            else if (terminal.userKeyMap.containsKey(input_uname)) {
                System.out.println("密码错误！");
                return;
            }
            else {
                System.out.println("用户不存在，为您自动注册");
                terminal.setUser2(new UserInfo(input_uname));
                String pwd = input_uname + input_pwd;
                terminal.userKeyMap.put(input_uname, pwd.hashCode());
                terminal.userInfoMap.put(input_uname, terminal.getUser2());
            }
        } else if (input_gtype.equalsIgnoreCase("3")) {
            terminal.setUser2(new UserInfo("游客"));
        } else if (input_gtype.equalsIgnoreCase("4")) {
            terminal.setUser2(new UserInfo("二级AI"));
        }

        terminal.setState(InitState.getInstance());

    }
}


class InitState implements TerminalState {
    private static final InitState instance = new InitState();
    private InitState() {}
    public static InitState getInstance() { return instance; }

    public void interfaceLogic(UserTerminal terminal) {
        Scanner scanner = new Scanner(System.in);
        int leng = 0;
        try {
            System.out.println("选择棋盘边长（大于等于8，且小于等于19）");
            String length = scanner.nextLine();
            leng = Integer.parseInt(length);
            assert (leng <= 19 && leng >= 8);
            terminal.setState(new ChooseModeState(leng));

        } catch (Exception e) {
            System.out.println(e);
        }
    }
}

class ChooseModeState implements TerminalState {
    private int leng;
    
    ChooseModeState(int length){ this.leng = length; }

    public void interfaceLogic(UserTerminal terminal) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("选择游戏类型：1为五子棋，2为围棋，3为黑白棋");
        String input_gtype = scanner.nextLine();
        if (!input_gtype.equalsIgnoreCase("1") && !input_gtype.equalsIgnoreCase("2") && !input_gtype.equalsIgnoreCase("3"))  {
            System.out.println("非法输入！目前仅支持五子棋、围棋、黑白棋，请重新选择游戏类型！");
            return;
        }
        if (input_gtype.equalsIgnoreCase("1") || input_gtype.equalsIgnoreCase("2")) {
            if (terminal.getUser1().getUserName().equalsIgnoreCase("二级AI")) {
                terminal.setUser1(new UserInfo("AI"));
            } 
            if (terminal.getUser2().getUserName().equalsIgnoreCase("二级AI")) {
                terminal.setUser2(new UserInfo("AI"));
            } 
        }
        if (input_gtype.equalsIgnoreCase("1")) {
            terminal.setGame(new WuZi(leng));
            terminal.setState(new WuZiPlayState());
        } else if (input_gtype.equalsIgnoreCase("2")) {
            terminal.setGame(new GoGame(leng));
            terminal.setState(new GoPlayState());
        } else if (input_gtype.equalsIgnoreCase("3")) {
            terminal.setGame(new Reversi());
            terminal.setState(new ReversiPlayState());
        }

    }
}

abstract class PlayState implements TerminalState {

    protected String menuString = "请选择操作：0为重新开始游戏；1为落子；2为保存当前对局；3为从文件读取存档覆盖当前局面；4为悔棋（仅限一步）；5为认输";

    public void interfaceLogic(UserTerminal terminal) {
        System.out.println("\n\n\n");
        terminal.getGame().printBoard();
        char player = terminal.getGame().getCurrentPlayer();
        String curPlayer = "";
        if (player == 'W') curPlayer = "白棋";
        else curPlayer = "黑棋";

        Scanner scanner = new Scanner(System.in);
        System.out.println("现在是" + curPlayer + "的回合");
        terminal.printUserInfo();

        if ((curPlayer == "白棋" && terminal.getUser1().getUserName().equalsIgnoreCase("AI")) || (curPlayer == "黑棋" && terminal.getUser2().getUserName().equalsIgnoreCase("AI"))) {
            System.out.println("AI正在思考中...");

            Random rd = new Random(1234);

            Vector<String> possiblePlays = this.possiblePlay(terminal);
            System.out.println(possiblePlays);

            Vector<String> possibleBetterPlays = this.possibleBetterPlay(terminal);
            System.out.println(possibleBetterPlays);

            String play = possiblePlays.get(rd.nextInt(possiblePlays.size()));
            String[] position = play.split(",");
            int x = Integer.parseInt(position[0]);
            int y = Integer.parseInt(position[1]);
            System.out.println(curPlayer + "(AI)落子于：(" + (char)((int)'A' + x) + "," + y + ")");
            goAndCheck(x, y, terminal, curPlayer);

            return ;
        } else if ((curPlayer == "白棋" && terminal.getUser1().getUserName().equalsIgnoreCase("二级AI")) || (curPlayer == "黑棋" && terminal.getUser2().getUserName().equalsIgnoreCase("二级AI"))) {
            System.out.println("二级AI正在思考中...");

            Random rd = new Random(1234);

            Vector<String> possiblePlay = this.possiblePlay(terminal);
            System.out.println(possiblePlay);

            Vector<String> possibleBetterPlays = this.possibleBetterPlay(terminal);
            System.out.println(possibleBetterPlays);

            String play = possibleBetterPlays.get(rd.nextInt(possibleBetterPlays.size()));
            String[] position = play.split(",");
            int x = Integer.parseInt(position[0]);
            int y = Integer.parseInt(position[1]);
            System.out.println(curPlayer + "(二级AI)落子于：(" + (char)((int)'A' + x) + "," + y + ")");
            goAndCheck(x, y, terminal, curPlayer);

            return ;
        } 

        System.out.println(menuString);
        String input = scanner.nextLine();

        switch (input) {
            case "0":
                terminal.getGame().restart();
                break;

            case "1":
                System.out.println("请以'x,y'形式输入要落子的坐标(例如: A,0)");
                String positions = scanner.nextLine();
                try {
                    String[] position = positions.split(",");
                    int x = (int)(char)(position[0].charAt(0)) - (int)'A';
                    int y = Integer.parseInt(position[1]);
                    goAndCheck(x, y, terminal, curPlayer);
                        
                } catch (Exception e) {
                    System.out.println("输入有误!");
                }
                break;

            case "2":
                terminal.getChessBoardManager().createMemo();
                System.out.println("请输入要保存的文件名");
                String save_path = scanner.nextLine();
                terminal.getChessBoardManager().save(save_path);
                System.out.println("保存成功！");
                terminal.saveRecord();
               
                break;

            case "3":
                System.out.println("请输入要读取的文件名");
                String load_path = scanner.nextLine();
                terminal.getChessBoardManager().load(load_path);
                if (recordCorrect(terminal)) {
                    terminal.setGame(newGame(terminal));
                } else {
                    System.out.println("读取错误！");
                }
                break;

            case "4":
                regret(terminal);
                break;

            case "5":
                System.out.println(curPlayer + "认输！游戏结束！");
                String winner = curPlayer.equalsIgnoreCase("白棋") ? "黑棋" : "白棋";
                terminal.gameOverUpdate(winner);

                terminal.setState(SystemModeState.getInstance());
                break;

            default:
                otherLogic(input, terminal, curPlayer);
            break;
        }

    }

    public Vector<String> possiblePlay(UserTerminal terminal) {
        int chessboardLength = terminal.getGame().chessboardLength;
        char[][] board = terminal.getGame().board;
        Vector<String> possiblePlays = new Vector<String>();

        for (int i = 0; i < chessboardLength; i++) {
            for (int j = 0; j < chessboardLength; j++) {
                if (board[i][j] == '-') {
                    if (terminal.getGame().validPlay(i, j)) {
                        possiblePlays.add(String.valueOf(i) + "," + String.valueOf(j));
                    }
                }
            }
        }
        return possiblePlays;
    }

    abstract public Vector<String> possibleBetterPlay(UserTerminal terminal);

    abstract public boolean recordCorrect(UserTerminal terminal);
    abstract public ChessGame newGame(UserTerminal terminal);
    abstract public void regret(UserTerminal terminal);
    // abstract public void surrender();
    abstract public void goAndCheck(int x, int y, UserTerminal terminal, String curPlayer);
    abstract void otherLogic(String command, UserTerminal terminal, String curPlayer);

}



class WuZiPlayState extends PlayState {

    public boolean recordCorrect(UserTerminal terminal) {
        Memento memo = (Memento) terminal.getChessBoardManager().getMemento();

        return memo.getGameType().equalsIgnoreCase("WuZi");
    }

    public ChessGame newGame(UserTerminal terminal) {
        return new WuZi(terminal.getChessBoardManager().getMemento());
    }

    public void goAndCheck(int x, int y, UserTerminal terminal, String curPlayer) {
        WuZi wuziGame = (WuZi) terminal.getGame();
        if (wuziGame.makeMove(x, y)) {
            terminal.addGameRecord(wuziGame.arr2str(wuziGame.board));
            if (wuziGame.checkFinish(x, y)) {
                wuziGame.printBoard();
                System.out.println("游戏结束！" + curPlayer + "获胜！");
                terminal.gameOverUpdate(curPlayer);

                terminal.setState(SystemModeState.getInstance());
            } else if (!wuziGame.checkPlayable()) {
                wuziGame.printBoard();
                System.out.println("游戏结束！平局！");
                terminal.gameOverUpdate("平局");

                terminal.setState(SystemModeState.getInstance());
            } else {
                wuziGame.switchPlayer();
            }
        } else {
            System.out.println("不能在此处落子，请重新操作！");
        }

    }

    public void regret(UserTerminal terminal) {
        if(terminal.getGame().regret()) {
            System.out.println("悔棋成功！");
            terminal.addGameRecord(terminal.getGame().arr2str(terminal.getGame().board));
        } else {
            System.out.println("不能悔棋！");
        }
    }

    public void otherLogic(String command, UserTerminal terminal, String curPlayer) {
        System.out.println("非法输入，请重新选择操作！（只能选择0-5中的操作）");
        return;
    }
    public Vector<String> possibleBetterPlay(UserTerminal terminal) {
        return this.possiblePlay(terminal);
    }

}

class GoPlayState extends PlayState {

    // public void interfaceLogic(UserTerminal terminal) {
    //     System.out.println(menuString);

    // }
    public GoPlayState() {
        this.menuString = "请选择操作：0为重新开始游戏；1为落子；2为保存当前对局；3为从文件读取存档覆盖当前局面；4为悔棋（仅限一步）；5为认输；6为不落子/虚着（双方均不落子时游戏进入胜负结算）";
    }

    public void goAndCheck(int x, int y, UserTerminal terminal, String curPlayer) {
        GoGame goGame = (GoGame) terminal.getGame();

        if (goGame.makeMove(x, y)) {
            if(goGame.getPassPlayer().equalsIgnoreCase(curPlayer)) goGame.setPassOnce(false);

            goGame.switchPlayer();
            terminal.addGameRecord(terminal.getGame().arr2str(terminal.getGame().board));

            if (!goGame.checkPlayable()) {
                goGame.printBoard();
                System.out.println("对方无棋可下，游戏结束！正在计算游戏结果......");
                String winner = goGame.checkWinner();
                System.out.println(winner + "获胜！");
                terminal.gameOverUpdate(winner);

                terminal.setState(SystemModeState.getInstance());
            }

        } else {
            System.out.println("不能在此处落子，请重新操作！");
        }

    }

    public boolean recordCorrect(UserTerminal terminal) {
        Memento memo = (Memento) terminal.getChessBoardManager().getMemento();

        return memo.getGameType().equalsIgnoreCase("Go");
    }

    public ChessGame newGame(UserTerminal terminal) {
        return new GoGame(terminal.getChessBoardManager().getMemento());
    }

    public void regret(UserTerminal terminal) {
        GoGame goGame = (GoGame) terminal.getGame();
        if(goGame.regret()) {
            System.out.println("悔棋成功！");
            terminal.addGameRecord(goGame.arr2str(goGame.board));
            goGame.setPassOnce(false);
        } else {
            System.out.println("不能悔棋！");
        }
    }

    public void otherLogic(String command, UserTerminal terminal, String curPlayer) {
        GoGame goGame = (GoGame) terminal.getGame();

        if (command.equalsIgnoreCase("6")) {
            if (goGame.getPassOnce()) {
                if (!goGame.getPassPlayer().equalsIgnoreCase(curPlayer)) {
                    System.out.println("双方均虚着，游戏结束！正在计算游戏结果......");

                    String winner = goGame.checkWinner();
                    System.out.println(winner + "获胜！");
                    terminal.gameOverUpdate(winner);

                    terminal.setState(SystemModeState.getInstance());
                    return;
                } else {
                    System.out.println(curPlayer + "连续虚着，视为认输，游戏结束！");
                    String winner = curPlayer.equalsIgnoreCase("白棋") ? "黑棋" : "白棋";
                    terminal.gameOverUpdate(winner);

                    terminal.setState(SystemModeState.getInstance());
                    return;
                }
            } else {
                goGame.setPassOnce(true);
                goGame.setPassPlayer(curPlayer);
                goGame.maintainMemory();
                goGame.switchPlayer();
            }

        } else {
            System.out.println("非法输入，请重新选择操作！（只能选择0-6中的操作）");
            return;
        }

        return;
    }

    public Vector<String> possibleBetterPlay(UserTerminal terminal) {
        return this.possiblePlay(terminal);
    }

}

class ReversiPlayState extends PlayState {

    public ReversiPlayState() {
        this.menuString = "请选择操作：0为重新开始游戏；1为落子；2为保存当前对局；3为从文件读取存档覆盖当前局面；4为悔棋（仅限一步）；5为认输";
    }
    public Vector<String> possibleBetterPlay(UserTerminal terminal) {
        Reversi reversiGame = (Reversi) terminal.getGame();
        int chessboardLength = reversiGame.chessboardLength;
        char[][] board = reversiGame.board;
        Vector<String> possiblePlays = new Vector<String>();
        int min_cnt = 64;
        double min_distance_from_center = 7;
        double max_distance_from_center = 0;

        for (int i = 0; i < chessboardLength; i++) {
            for (int j = 0; j < chessboardLength; j++) {
                if (board[i][j] == '-') {
                    if (reversiGame.validPlay(i, j)) {
                        if ((i == 0 || i == 7) && (j == 0 || j == 7)) {
                            possiblePlays.clear();
                            possiblePlays.add(String.valueOf(i) + "," + String.valueOf(j));
                            return possiblePlays;
                        }
                        reversiGame.tryMove(i, j);
                        int cnt = reversiGame.countRivalPossibleSteps(reversiGame.getCurrentPlayer());
                        double distance = Math.abs(i - 3.5) + Math.abs(j - 3.5);
                        if (distance >= 5) distance = 0;
                        reversiGame.restoreBoard();

                        if (cnt < min_cnt) {
                            min_cnt = cnt;
                            max_distance_from_center = distance;
                            possiblePlays.clear();
                            possiblePlays.add(String.valueOf(i) + "," + String.valueOf(j));
                        }
                        else if (cnt == min_cnt) {
                            if (distance > min_distance_from_center) {
                                max_distance_from_center = distance;
                                possiblePlays.clear();
                                possiblePlays.add(String.valueOf(i) + "," + String.valueOf(j));
                            } else if (distance == max_distance_from_center) {
                                possiblePlays.add(String.valueOf(i) + "," + String.valueOf(j));
                            }
                        }
                    }
                }
            }
        }

        return possiblePlays;
    }

    public void goAndCheck(int x, int y, UserTerminal terminal, String curPlayer) {
        Reversi reversiGame = (Reversi) terminal.getGame();

        if (reversiGame.makeMove(x, y)) {
            reversiGame.switchPlayer();
            terminal.addGameRecord(reversiGame.arr2str(reversiGame.board));
            if (!reversiGame.checkPlayable()) {
                System.out.println("对方无棋可下，跳过！");
                reversiGame.switchPlayer();
                if (!reversiGame.checkPlayable()) {
                    reversiGame.printBoard();
                    System.out.println("双方均无棋可下，游戏结束！");
                    String winner = reversiGame.checkWinner();
                    if (winner.equalsIgnoreCase("平局")) {
                        System.out.println("游戏结束！平局！");
                    } else {
                        System.out.println(winner + "获胜！");
                    }
                    terminal.gameOverUpdate(winner);

                    terminal.setState(SystemModeState.getInstance());
                }
            }

        } else {
            System.out.println("不能在此处落子，请重新操作！");
        }

    }


    public boolean recordCorrect(UserTerminal terminal) {
        Memento memo = (Memento) terminal.getChessBoardManager().getMemento();

        return memo.getGameType().equalsIgnoreCase("Reversi");
    }

    public ChessGame newGame(UserTerminal terminal) {
        return new Reversi(terminal.getChessBoardManager().getMemento());
    }

    public void regret(UserTerminal terminal) {
        if(terminal.getGame().regret()) {
            terminal.addGameRecord(terminal.getGame().arr2str(terminal.getGame().board));
            System.out.println("悔棋成功！");
        } else {
            System.out.println("不能悔棋！");
        }
    }

    public void otherLogic(String command, UserTerminal terminal, String curPlayer) {
        System.out.println("非法输入，请重新选择操作！（只能选择0-5中的操作）");
        return;
    }


}

class UserInfo implements Serializable{
    protected String userName;
    protected int playedGames;
    protected int wonGames;
    
    public static boolean checkUserInfo(String userName, String userPassword, HashMap<String, Integer> userKeyMap) {
        // 检查用户名和密码是否匹配
        // 返回 true 或 false
        String userInfo = userName + userPassword;
        
        return userInfo.hashCode() == userKeyMap.get(userName);
    }

    UserInfo(String userName) {
        this.userName = userName;
        this.playedGames = 0;
        this.wonGames = 0;
    }

    public void addPlayedGames() {this.playedGames++;}
    public void addWonGames() {this.wonGames++;}

    public String getUserName() {return this.userName;}
    public int getPlayedGames() {return this.playedGames;}
    public int getWonGames() {return this.wonGames;}

}


class UserTerminal {
    private ChessGame game;
    private TerminalState state = SystemModeState.getInstance();
    private ChessBoardManager boardManager = new ChessBoardManager();
    HashMap<String, Integer> userKeyMap = new HashMap<String, Integer>();
    HashMap<String, UserInfo> userInfoMap = new HashMap<String, UserInfo>();
    UserInfo user1, user2;
    Vector<String> gameRecord = new Vector<String>();

    public void printUserInfo() {
        String username1 = this.user1.getUserName();
        String username2 = this.user2.getUserName();
        printUserInfo(username1, username2);

    }

    public void printUserInfo(String user1, String user2) {
        UserInfo userinfo1 = userInfoMap.get(user1);
        UserInfo userinfo2 = userInfoMap.get(user2);
        if (user1.equalsIgnoreCase("AI") || user1.equalsIgnoreCase("二级AI") || user1.equalsIgnoreCase("游客")) {
            System.out.println("User1（白棋）: " + user1 + ", Played: " + 0 + ", Won: " + 0);
        } else {
            System.out.println("User1（白棋）: " + userinfo1.getUserName() + ", Played: " + userinfo1.getPlayedGames() + ", Won: " + userinfo1.getWonGames());
        }
        if (user2.equalsIgnoreCase("AI") || user2.equalsIgnoreCase("二级AI") || user2.equalsIgnoreCase("游客")) {
            System.out.println("User2（黑棋）: " + user2 + ", Played: " + 0 + ", Won: " + 0);
        } else {
            System.out.println("User2（黑棋）: " + userinfo2.getUserName() + ", Played: " + userinfo2.getPlayedGames() + ", Won: " + userinfo2.getWonGames());
        }
        
    }

    public void setUser1(UserInfo user1) {
        this.user1 = user1;
    }

    public void setUser2(UserInfo user2) {
        this.user2 = user2;
    }

    public UserInfo getUser1() {
        return this.user1;
    }

    public UserInfo getUser2() {
        return this.user2;
    }

    public void addGameRecord(String board) {
        this.gameRecord.add(board);
    }

    public void gameOverUpdate(String winner) {
        user1.addPlayedGames();
        user2.addPlayedGames();
        if (winner.equalsIgnoreCase("白棋")) {
            user1.addWonGames();
        } else if (winner.equalsIgnoreCase("黑棋")) {
            user2.addWonGames();
        }
        if (!user1.getUserName().equalsIgnoreCase("AI") && !user1.getUserName().equalsIgnoreCase("游客") && !user1.getUserName().equalsIgnoreCase("二级AI")) userInfoMap.put(user1.userName, user1);
        if (!user2.getUserName().equalsIgnoreCase("AI") && !user2.getUserName().equalsIgnoreCase("二级AI") && !user2.getUserName().equalsIgnoreCase("游客")) userInfoMap.put(user2.userName, user2);

        saveUserInfo();
        saveRecord();
    }

    public void saveRecord() {
        try {
            long timestamp = new Date().getTime();

            String record_path = Long.toString(timestamp) + ".ser";
            FileOutputStream record_fos = new FileOutputStream(record_path);
            ObjectOutputStream record_oos = new ObjectOutputStream(record_fos);
            Vector<String> gameRecordClone = (Vector<String>) this.gameRecord.clone();

            gameRecordClone.add(user1.getUserName());
            gameRecordClone.add(user2.getUserName());
            gameRecordClone.add(Integer.toString(this.game.chessboardLength));

            record_oos.writeObject(gameRecordClone);
            record_oos.close();
            record_fos.close();
            System.out.println("已将录像保存至" + record_path + "\n可通过回放模式观看");
        } catch (FileNotFoundException e) {
            System.out.println(e);
        } catch (IOException e) {
            System.out.println(e);
        }
    }


    public void saveUserInfo() {
        try {
            String key_path = new String("userkeymap.ser");
            FileOutputStream key_fos = new FileOutputStream(key_path);
            ObjectOutputStream key_oos = new ObjectOutputStream(key_fos);
            key_oos.writeObject(this.userKeyMap);
            key_oos.close();
            key_fos.close();

            String info_path = new String("userinfomap.ser");
            FileOutputStream info_fos = new FileOutputStream(info_path);
            ObjectOutputStream info_oos = new ObjectOutputStream(info_fos);
            info_oos.writeObject(this.userInfoMap);
            info_oos.close();
            info_fos.close();
        } catch (FileNotFoundException e) {
            System.out.println(e);
        } catch (IOException e) {
            System.out.println(e);
        }

    }

    public void loadUserInfo() {
        try {
            String key_path = new String("userkeymap.ser");
            FileInputStream key_fis = new FileInputStream(key_path);
            ObjectInputStream key_ois = new ObjectInputStream(key_fis);
            this.userKeyMap = (HashMap<String, Integer>) key_ois.readObject();

            key_ois.close();
            key_fis.close();

            String info_path = new String("userinfomap.ser");
            FileInputStream info_fis = new FileInputStream(info_path);
            ObjectInputStream info_ois = new ObjectInputStream(info_fis);
            this.userInfoMap = (HashMap<String, UserInfo>) info_ois.readObject();

            info_ois.close();
            info_fis.close();

        } catch (FileNotFoundException e) {
            System.out.println(e);
        } catch (IOException e) {
            System.out.println(e);
        } catch (ClassNotFoundException e) {
            System.out.println(e);
        }
    }
    

    public void play() {
        loadUserInfo();

        while(true) {
            this.state.interfaceLogic(this);
        }
    }

    public void setState(TerminalState state) { 
        System.out.println("\n\n\n");
        this.state = state; 
    }
    public void setGame(ChessGame game) { 
        this.game = game;
        this.boardManager.setGame(game);
    }

    public ChessGame getGame() { return this.game; }
    public ChessBoardManager getChessBoardManager() { return this.boardManager; }

}


public class ChessGamePlatform {
    private static final UserTerminal terminal = new UserTerminal();
    public static void main(String[] args) {
        terminal.play();
    }
    
}