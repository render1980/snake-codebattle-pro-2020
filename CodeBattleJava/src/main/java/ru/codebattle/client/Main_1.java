package ru.codebattle.client;

import lombok.extern.slf4j.Slf4j;
import ru.codebattle.client.api.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Random;

import static java.util.Arrays.asList;
import static ru.codebattle.client.api.BoardElement.*;

/**
 * + Хорошо шарит по углам в одном секторе, избегает препятствия.
 * - Трусливая стратегия, не видит далеко
 */
@Slf4j
public class Main_1 {
    private static final String SERVER_ADDRESS = "http://codebattle-pro-2020s1.westeurope.cloudapp.azure.com/codenjoy-contest/board/player/zszo1285csm88vae76ph?code=7400189051783371666&gameName=snakebattle";

    public static void main(String[] args) throws URISyntaxException, IOException {
        SnakeBattleClient client = new SnakeBattleClient(SERVER_ADDRESS);
        client.run(gameBoard -> {
            return moveToFirstBestCell(gameBoard);
        });

        System.in.read();

        client.initiateExit();
    }

    /************
     * STRATEGY *
     ************/

//    /**
//     * Перемещение в сторону, куда удобнее всего
//     */
//    public static SnakeAction moveToFirstBestCell(GameBoard gameBoard) {
//        BoardPoint myHead = gameBoard.getMyHead();
//        int myX = myHead.getX();
//        int myY = myHead.getY();
//        log.debug(String.format("My position: {%d,%d}", myX, myY));
//
//        // TODO: искать рекурсивно в других клетках
//
//        /**
//         * Ищем ништяки в порядке приоритета на соседних клетках.
//         * Если нашли - проверяем, что целевая клетка не является ловушкой.
//         * Если там ловушка - то пытаемся сыбаццо в одном из двух возможных направлений
//         */
//        List<BoardElement> goods = asList(GOLD, APPLE, FLYING_PILL, FURY_PILL, NONE);
//        for (BoardElement good : goods) {
//            Direction direction = gameBoard.searchNeighborElementDirection(good);
//            if (direction != null) {
//                // на ближайших клетках есть ништяки
//                Direction checkedDirection = gameBoard.checkNextPointIsTrap(direction);
//                log.debug(good.name() + " on " + direction.name() + " Go to " + checkedDirection.name());
//                return new SnakeAction(false, checkedDirection);
//            }
//        }
//
//        return new SnakeAction(true, Direction.STOP);
//    }

    /**
     * Перемещение в сторону, куда удобнее всего
     */
    public static SnakeAction moveToFirstBestCell(GameBoard gameBoard) {
        BoardPoint myHead = gameBoard.getMyHead();
        int myX = myHead.getX();
        int myY = myHead.getY();
        log.debug(String.format("My position: {%d,%d}", myX, myY));

        // TODO: искать рекурсивно в других клетках

        /**
         * Ищем ништяки в порядке приоритета на соседних клетках.
         * Если нашли - проверяем, что целевая клетка не является ловушкой.
         * Если там ловушка - то пытаемся сыбаццо в одном из двух возможных направлений
          */
        List<BoardElement> goods = asList(GOLD, APPLE, FLYING_PILL, FURY_PILL);
        for (BoardElement good : goods) {
            Direction direction = gameBoard.searchNeighborElementDirection(good);
            if (direction != null) {
                // на ближайших клетках есть ништяки
                Direction checkedDirection = gameBoard.checkNextPointIsTrap(direction);
                log.debug(good.name() + " on " + direction.name() + " Go to " + checkedDirection.name());
                return new SnakeAction(false, checkedDirection);
            }
        }

        // пробуем идти туда куда шли, чтобы не петлять
//        gameBoard.searchNeighborElementDirectionBy(, NONE);

        // если нет на ближайших - может быть есть в пределах прямой видимости?


        // если уж совсем ничего в пределах видимости => пойдем на пустую клетку
        Direction noneDirection = gameBoard.searchNeighborElementDirection(NONE);
        if (noneDirection != null) {
            // на ближайших клетках есть ништяки
            Direction checkedDirection = gameBoard.checkNextPointIsTrap(noneDirection);
            log.debug(NONE.name() + " on " + noneDirection.name() + " Go to " + checkedDirection.name());
            return new SnakeAction(false, checkedDirection);
        }

        return new SnakeAction(true, Direction.STOP);
    }

    public static SnakeAction trySafetyMove(BoardElement element, Direction direction) {
        if (element == null || direction == null) {
            return null;
        }
        if (asList(APPLE, GOLD, FLYING_PILL, FURY_PILL, NONE).contains(element)) {
            return new SnakeAction(false, direction);
        }
        return null;
    }

    public static Direction randomDirection() {
        var       random = new Random(System.currentTimeMillis());
        Direction dir    = Direction.values()[random.nextInt(Direction.values().length)];
        // var act       = random.nextInt() % 2 == 0;
        return dir;
    }
}
