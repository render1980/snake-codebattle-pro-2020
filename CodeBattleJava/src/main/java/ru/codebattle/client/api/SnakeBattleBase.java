package ru.codebattle.client.api;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
public abstract class SnakeBattleBase
        extends WebSocketClient {

    private final String responsePrefix = "board=";

    public SnakeBattleBase(String url) throws URISyntaxException {
        super(new URI(url.replace("http", "ws").replace("board/player/", "ws?user=").replace("?code=", "&code=")));
    }

    @Setter
    @Getter
    private boolean shouldExit;

    protected abstract String doMove(GameBoard gameBoard);

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        log.info("Connection established");
    }

    @Override
    public void onMessage(String message) {
        if (!shouldExit) {

            if (!message.startsWith(responsePrefix)) {
                log.error(String.format("Something strange is happening on the server... Response:%n%s", message));
                shouldExit = true;
            } else {
                String boardString = message.substring(responsePrefix.length());
                String action = doMove(new GameBoard(boardString));
                send(action);
            }
        }

    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.warn("### disconnected ###");
    }

    @Override
    public void onError(Exception ex) {
        log.error("### error ###", ex);
    }
}
