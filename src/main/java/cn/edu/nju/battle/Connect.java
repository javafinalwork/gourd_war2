package cn.edu.nju.battle;

import cn.edu.nju.SceneSwitch;
import cn.edu.nju.constant.Constant;
import javafx.application.Platform;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.lang.Runnable;

class BaseConnector implements Runnable
{
    SceneSwitch ss;
    Battlefield battlefield;

    BaseConnector(SceneSwitch ss, Battlefield battlefield)
    {
        this.ss = ss;
        this.battlefield = battlefield;
    }

    public void setBattlefield(Battlefield battlefield)
    {
        this.battlefield = battlefield;
    }


    @Override
    public void run()
    {

    }

    public void write(BattleMsg msg) throws IOException
    {

    }
}


class DataServer extends BaseConnector
{
    ObjectOutputStream serverOos;
    ObjectInputStream clientOis;
    int mapId;

    DataServer(SceneSwitch ss, Battlefield battlefield, int mapId)
    {
        super(ss, battlefield);
        this.mapId = mapId;
    }

    @Override
    public void run()
    {
        try
        {
            startServer();
        } catch (IOException | ClassNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    private void startServer() throws IOException, ClassNotFoundException
    {
        ServerSocket server = new ServerSocket(Constant.portNumber);
        Socket clientSocket = server.accept();

        clientOis = new ObjectInputStream(clientSocket.getInputStream());
        serverOos = new ObjectOutputStream(clientSocket.getOutputStream());
        // 回调FX函数在非FX线程
        Platform.runLater(() -> ss.finishConnect(mapId));
        write(new MapMsg(mapId, true, 0));

        BattleMsg msg = null;

        while ((msg = (BattleMsg) clientOis.readObject()) != null)
        {
            if (msg.msgType == MsgType.FINISH_MSG)
            {
                final boolean isCalabashWin = msg.isCalabashWin();
                final boolean isMonsterWin = msg.isMonsterWin();

                clientSocket.shutdownInput();
                clientSocket.shutdownOutput();
                clientSocket.close();
                Platform.runLater(() -> ss.changeToFinishScene(isCalabashWin, isMonsterWin));
                break;
            }
            battlefield.parseMsg(msg);
        }
    }

    @Override
    public void write(BattleMsg msg) throws IOException
    {
        serverOos.writeObject(msg);
//        serverOos.flush();
    }
}


class DataClient extends BaseConnector
{
    String ipAddr;
    ObjectOutputStream clientOos;
    ObjectInputStream serverOis;

    DataClient(SceneSwitch ss, Battlefield battlefield, String ipAddr)
    {
        super(ss, battlefield);
        this.ipAddr = ipAddr;
    }


    @Override
    public void run()
    {
        try
        {
            startClient();
        } catch (IOException | ClassNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    private void startClient() throws IOException, ClassNotFoundException
    {
        Socket echoSocket = new Socket(ipAddr, Constant.portNumber);

        clientOos = new ObjectOutputStream(echoSocket.getOutputStream());
        serverOis = new ObjectInputStream(echoSocket.getInputStream());
        BattleMsg msg;

        try
        {
            while ((msg = (BattleMsg) serverOis.readObject()) != null)
            {
                if (msg.msgType == MsgType.FINISH_MSG)
                {
                    final boolean isCalabashWin = msg.isCalabashWin();
                    final boolean isMonsterWin = msg.isMonsterWin();
                    echoSocket.shutdownInput();
                    //echo msg
                    write(new FinishMsg(isCalabashWin, isMonsterWin, false, 0));
                    echoSocket.shutdownOutput();
                    echoSocket.close();
                    Platform.runLater(() -> ss.changeToFinishScene(isCalabashWin, isMonsterWin));
                    break;
                }
                else if (msg.msgType == MsgType.MAP_MSG)
                {
                    final int mapId = msg.getMapId();
                    Platform.runLater(() -> ss.finishConnect(mapId));
                }
                else
                {
                    battlefield.parseMsg(msg);
                }
            }
        } catch (EOFException ignored)
        {
        }
    }

    @Override
    public void write(BattleMsg msg) throws IOException
    {
//        clientOos.reset();
        clientOos.writeObject(msg);
//        clientOos.flush();
    }


}
