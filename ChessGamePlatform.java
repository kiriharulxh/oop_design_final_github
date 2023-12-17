import java.util.*;
import java.io.*;



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
            if (findQi(x, y, currentPlayer)) { // 未考虑打劫
                return true;
            }
        }
        return false;
    }

    @Override
    public void changeBoard(int x, int y) {
        super.changeBoard(x, y);
        eatDeadChess(x, y);
    }

    @Override
    public boolean checkPlayable() {
        for (int i = 0; i < chessboardLength; i++) {
            for (int j = 0; j < chessboardLength; j++) {
                if (board[i][j] == '-') {
                    if (findQi(i, j, currentPlayer)) return true;
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

interface TerminalState {
    void interfaceLogic(UserTerminal terminal); 
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
        System.out.println("选择游戏类型：1为五子棋，2为围棋");
        String input_gtype = scanner.nextLine();
        if (!input_gtype.equalsIgnoreCase("1") && !input_gtype.equalsIgnoreCase("2"))  {
            System.out.println("非法输入！目前仅支持五子棋和围棋，请重新选择游戏类型！");
            return;
        }

        if (input_gtype.equalsIgnoreCase("1")) {
            terminal.setGame(new WuZi(leng));
            terminal.setState(new WuZiPlayState());

        } else if (input_gtype.equalsIgnoreCase("2")) {
            terminal.setGame(new GoGame(leng));
            terminal.setState(new GoPlayState());
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
                    System.out.println(e);
                    System.out.println("输入有误!");
                }
                break;

            case "2":
                terminal.getChessBoardManager().createMemo();
                System.out.println("请输入要保存的文件名");
                String save_path = scanner.nextLine();
                terminal.getChessBoardManager().save(save_path);
                System.out.println("保存成功！");
               
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
                terminal.setState(InitState.getInstance());
                break;

            default:
                otherLogic(input, terminal, curPlayer);
            break;
        }

    }

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
            if (wuziGame.checkFinish(x, y)) {
                System.out.println("游戏结束！" + curPlayer + "获胜！");
                terminal.setState(InitState.getInstance());
            } else if (!wuziGame.checkPlayable()) {
                System.out.println("游戏结束！平局！");
                terminal.setState(InitState.getInstance());
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
        } else {
            System.out.println("不能悔棋！");
        }
    }

    public void otherLogic(String command, UserTerminal terminal, String curPlayer) {
        System.out.println("非法输入，请重新选择操作！（只能选择0-5中的操作）");
        return;
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
            if (!goGame.checkPlayable()) {
                System.out.println("对方无棋可下，游戏结束！正在计算游戏结果......");
                String winner = goGame.checkWinner();
                System.out.println(winner + "获胜！");
                terminal.setState(InitState.getInstance());
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
                    terminal.setState(InitState.getInstance());
                    return;
                } else {
                    System.out.println(curPlayer + "连续虚着，视为认输，游戏结束！");
                    terminal.setState(InitState.getInstance());
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

}



class UserTerminal {
    private ChessGame game;
    private TerminalState state = InitState.getInstance();
    private ChessBoardManager boardManager = new ChessBoardManager();

    public void play() {
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