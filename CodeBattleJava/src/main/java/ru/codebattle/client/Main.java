package ru.codebattle.client;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

import lombok.extern.slf4j.Slf4j;
import ru.codebattle.client.api.*;

import static ru.codebattle.client.api.BoardElement.*;

@Slf4j
public class Main {
    //    private static final String SERVER_ADDRESS = "http://localhost:8080/codenjoy-contest/board/player/6mlolfpaekvspk868rdh?code=1855478191833212450&gameName=snakebattle";
    private static final String SERVER_ADDRESS = "http://codebattle-pro-2020s1.westeurope.cloudapp.azure.com/codenjoy-contest/board/player/ckt68it0vbj5a0azt6ws?code=7215362297398155535&gameName=snakebattle";
//    private static final String SERVER_ADDRESS = ""

    private static Direction curDirection;
    private static Direction prevDirection;

    private static List<BoardPoint> prevPoints = new ArrayList<>();

    public static void main(String[] args) throws URISyntaxException, IOException {
        SnakeBattleClient client = new SnakeBattleClient(SERVER_ADDRESS);
        client.run(gameBoard -> {
            tryResetPrevPoint(gameBoard);
            SnakeAction snakeAction = moveToFirstBestCell(gameBoard);

            if (curDirection != null) {
                prevDirection = curDirection;
            }
            curDirection = snakeAction.getDirection();

            return snakeAction;
        });

        System.in.read();

        client.initiateExit();
    }

    private static void tryResetPrevPoint(GameBoard board) {
        BoardPoint head = board.getMyHead();
        BoardElement headElement = board.getElementAt(head);
        if (headElement == null) {
            return;
        }
        if (headElement.equals(START_FLOOR)) {
            prevPoints.clear();
        }
    }

    /************
     * STRATEGY *
     ************/

    /**
     * Перемещение в сторону, куда удобнее всего
     */
    public static SnakeAction moveToFirstBestCell(GameBoard gameBoard) {
        BoardPoint myHead = gameBoard.getMyHead();
        int myX = myHead.getX();
        int myY = myHead.getY();
        log.debug(String.format("My position: {%d,%d}", myX, myY));
        /**
         * Сначала посмотрим, есть ли ништяки на соседних клетках
         */
        Direction firstDirection = gameBoard.searchAnyGoodNeighborDirection();
        if (firstDirection != null) {
            Direction checkedDirection = gameBoard.checkNextPointIsTrap(firstDirection);
            log.debug(NONE.name() + " on " + firstDirection.name() + " Go to " + checkedDirection.name());
            refreshCurPoints(myHead, checkedDirection);
            return new SnakeAction(false, checkedDirection);
        }
        /**
         * Если поблизости нет - будем смотреть на все поле, куда идти
         *
         * Ищем ништяки на всем поле в порядке приоритета.
         * Если нашли - проверяем, что целевая клетка не является ловушкой.
         * Если там ловушка - то пытаемся сыбаццо в одном из двух возможных направлений
         */
        Direction direction = gameBoard.searchNearestGoodStepDir(curDirection);
        if (direction != null && gameBoard.isAcceptable(direction)) {
            log.debug("Direction to good: " + direction.toString());
            // как обойти ловушку
            Direction checkedDirection = gameBoard.checkNextPointIsTrap(direction);
            // TODO: обнаружение петель
            BoardPoint nextPoint = neighborPointByDir(myHead, checkedDirection);
            if (isLoop(nextPoint)) {
                Direction directionOpposite = oppositeDirection(checkedDirection);
                if (gameBoard.isAcceptable(neighborPointByDir(myHead, directionOpposite))) {
                    refreshCurPoints(gameBoard.getMyHead(), checkedDirection);
                    return new SnakeAction(false, directionOpposite);
                }
            }

            log.debug("Good " + " on " + direction.name() + " Go to " + checkedDirection.name());
            refreshCurPoints(myHead, checkedDirection);
            return new SnakeAction(false, checkedDirection);
        }

        /**
         * Ну или пробуем случайную клетку с NONE
         */
        Direction noneDirection = gameBoard.searchNeighborElementDirection(NONE);
        if (noneDirection != null) {
            Direction checkedDirection = gameBoard.checkNextPointIsTrap(noneDirection);
            log.debug(NONE.name() + " on " + noneDirection.name() + " Go to " + checkedDirection.name());
            refreshCurPoints(myHead, checkedDirection);
            return new SnakeAction(false, checkedDirection);
        }

        return new SnakeAction(true, Direction.STOP);
    }

    private static Direction randomDirection() {
        var random = new Random(System.currentTimeMillis());
        Direction dir = Direction.values()[random.nextInt(Direction.values().length)];
        // var act       = random.nextInt() % 2 == 0;
        return dir;
    }

    private static BoardPoint neighborPointByDir(BoardPoint head, Direction direction) {
        BoardPoint pointRight = head.shiftRight();
        BoardPoint pointUp = head.shiftTop();
        BoardPoint pointLeft = head.shiftLeft();
        BoardPoint pointDown = head.shiftBottom();
        Map<Direction, BoardPoint> dirToPoints = new HashMap<>() {{
            put(Direction.RIGHT, pointRight);
            put(Direction.LEFT, pointLeft);
            put(Direction.DOWN, pointDown);
            put(Direction.UP, pointUp);
        }};
        return dirToPoints.get(direction);
    }

    private static void refreshCurPoints(BoardPoint head, Direction direction) {
        BoardPoint nextPoint = neighborPointByDir(head, direction);
        if (prevPoints.size() > 6) {
            prevPoints.remove(0);
        }
        prevPoints.add(nextPoint);
    }

    private static boolean isLoop(BoardPoint nextPoint) {
        return prevPoints.contains(nextPoint);
    }

    private static Direction oppositeDirection(Direction direction) {
        Map<Direction, Direction> oppositeDirs = new HashMap<>() {{
            put(Direction.RIGHT, Direction.LEFT);
            put(Direction.LEFT, Direction.RIGHT);
            put(Direction.UP, Direction.DOWN);
            put(Direction.DOWN, Direction.UP);
        }};
        return oppositeDirs.get(direction);
    }
}
