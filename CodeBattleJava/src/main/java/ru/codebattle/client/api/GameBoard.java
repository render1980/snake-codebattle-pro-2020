package ru.codebattle.client.api;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static java.util.Arrays.asList;
import static ru.codebattle.client.api.BoardElement.*;
import static ru.codebattle.client.api.Direction.*;
import static ru.codebattle.client.api.Direction.DOWN;

import java.util.*;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

@Slf4j
public class GameBoard {

    private static final List<BoardElement> GOODS = asList(GOLD, APPLE, FLYING_PILL, FURY_PILL);
    private static final List<BoardElement> GOODS_WITH_POINTS = asList(GOLD, APPLE);
    private static final List<BoardElement> GOODS_WITH_NONE = asList(GOLD, APPLE, FLYING_PILL, FURY_PILL, NONE);

    public GameBoard(String boardString) {
        this.boardString = boardString.replace("\n", "");
    }

    @Getter
    private String boardString;

    public int size() {
        return (int) sqrt(boardString.length());
    }

    public BoardPoint getMyHead() {
        return findFirstElement(HEAD_DEAD, HEAD_DOWN, HEAD_UP, HEAD_LEFT, HEAD_RIGHT, HEAD_EVIL,
                HEAD_FLY, HEAD_SLEEP);
    }

    public List<BoardPoint> getWalls() {
        return findAllElements(WALL);
    }

    public List<BoardPoint> getStones() {
        return findAllElements(STONE);
    }

    public boolean isBarrierAt(BoardPoint point) {
        return getBarriers().contains(point);
    }

    public List<BoardPoint> getApples() {
        return findAllElements(APPLE);
    }

    public boolean amIEvil() {
        return findAllElements(HEAD_EVIL).contains(getMyHead());
    }

    public boolean amIFlying() {
        return findAllElements(HEAD_FLY).contains(getMyHead());
    }

    public List<BoardPoint> getFlyingPills() {
        return findAllElements(FLYING_PILL);
    }

    public List<BoardPoint> getFuryPills() {
        return findAllElements(FURY_PILL);
    }

    public List<BoardPoint> getGold() {
        return findAllElements(GOLD);
    }

    public List<BoardPoint> getStartPoints() {
        return findAllElements(START_FLOOR);
    }

    private List<BoardPoint> getBarriers() {
        return findAllElements(WALL, START_FLOOR, ENEMY_HEAD_SLEEP, ENEMY_TAIL_INACTIVE, TAIL_INACTIVE, STONE);
    }

    public boolean hasElementAt(BoardPoint point, BoardElement element) {
        if (point.isOutOfBoard(size())) {
            return false;
        }

        return getElementAt(point) == element;
    }

    public BoardElement getElementAt(BoardPoint point) {
        return BoardElement.valueOf(boardString.charAt(getShiftByPoint(point)));
    }

    public void printBoard() {
        for (int i = 0; i < size(); i++) {
            System.out.println(boardString.substring(i * size(), size() * (i + 1)));
        }
    }

    public BoardPoint findElement(BoardElement elementType) {
        for (int i = 0; i < size() * size(); i++) {
            BoardPoint pt = getPointByShift(i);
            if (hasElementAt(pt, elementType)) {
                return pt;
            }
        }
        return null;
    }

    public BoardPoint findFirstElement(BoardElement... elementType) {
        for (int i = 0; i < size() * size(); i++) {
            BoardPoint pt = getPointByShift(i);

            for (BoardElement elemType : elementType) {
                if (hasElementAt(pt, elemType)) {
                    return pt;
                }
            }
        }
        return null;
    }

    public List<BoardPoint> findAllElements(BoardElement... elementType) {
        List<BoardPoint> result = new ArrayList<>();

        for (int i = 0; i < size() * size(); i++) {
            BoardPoint pt = getPointByShift(i);

            for (BoardElement elemType : elementType) {
                if (hasElementAt(pt, elemType)) {
                    result.add(pt);
                }
            }
        }

        return result;
    }

    public List<BoardPoint> findAllNearestElements(BoardPoint from, List<BoardElement> elementType) {
        List<BoardPoint> result = new ArrayList<>();
        int headShift = getShiftByPoint(from);
//        for (int i = headShift - 1, j = headShift + 1; i >= 0 && j < size() * size(); i--, j++) {
//            BoardPoint topPoint = getPointByShift(i);
//            BoardPoint bottomPoint = getPointByShift(j);
//            for (BoardElement elemType : elementType) {
//                if (hasElementAt(topPoint, elemType)) {
//                    result.add(topPoint);
//                }
//                if (hasElementAt(bottomPoint, elemType)) {
//                    result.add(bottomPoint);
//                }
//            }
//        }

        for (int i = 0; i < size() * size(); i++) {
            BoardPoint pt = getPointByShift(i);

            for (BoardElement elemType : elementType) {
                if (hasElementAt(pt, elemType)) {
                    result.add(pt);
                }
            }
        }

        // сначала будут те точки, до которых расстояние от головы меньше
        result.sort((a, b) -> {
            double lengthFromToA = sqrt(pow(from.getX() - a.getX(), 2) + pow(from.getY() - a.getY(), 2));
            double lengthFromToB = sqrt(pow(from.getX() - b.getX(), 2) + pow(from.getY() - b.getY(), 2));
            if (lengthFromToA < lengthFromToB) {
                return -1;
            } else if (lengthFromToA > lengthFromToB) {
                return 1;
            } else {
                return 0;
            }
        });
        return result;
    }

//    public List<BoardPoint> findAllNearestElements(BoardPoint from, BoardElement... elementType) {
//        List<BoardPoint> result = new ArrayList<>();
//        int headShift = getShiftByPoint(from);
//        for (int i = headShift-1, j = headShift+1; i >= 0 && j < size() * size(); i--, j++) {
//            BoardPoint topPoint = getPointByShift(i);
//            BoardPoint bottomPoint = getPointByShift(j);
//            for (BoardElement elemType : elementType) {
//                if (hasElementAt(topPoint, elemType)) {
//                    result.add(topPoint);
//                }
//                if (hasElementAt(bottomPoint, elemType)) {
//                    result.add(bottomPoint);
//                }
//            }
//        }
//
//        return result;
//    }

    public boolean hasElementAt(BoardPoint point, BoardElement... elements) {
        return Arrays.stream(elements).anyMatch(element -> hasElementAt(point, element));
    }

    private int getShiftByPoint(BoardPoint point) {
        return point.getY() * size() + point.getX();
    }

    private BoardPoint getPointByShift(int shift) {
        return new BoardPoint(shift % size(), shift / size());
    }

    /**
     * Проверка, не окажемся ли мы на следующем ходу в ловушке
     *
     * Если да, то пытаемся уйти (куда - выбирается случайно из двух возможных направлений отхода)
     *
     * @return - возврат значения параметра direction, если ловушек нет
     */
    public Direction checkNextPointIsTrap(Direction direction) {
        BoardPoint myHead = getMyHead();

        BoardPoint targetPoint;
        switch (direction) {
            case UP:
                targetPoint = myHead.shiftTop();
                break;
            case DOWN:
                targetPoint = myHead.shiftBottom();
                break;
            case RIGHT:
                targetPoint = myHead.shiftRight();
                break;
            case LEFT:
                targetPoint = myHead.shiftLeft();
                break;
            default:
                return null;
        }

        /**
         * ***
         * *@*
         *  X
         *
         * Escapes: <>
         */
        if (asList(WALL, START_FLOOR, STONE).contains(getElementAt(targetPoint.shiftRight()))
                && asList(WALL, START_FLOOR, STONE).contains(getElementAt(targetPoint.shiftLeft()))
                && asList(WALL, START_FLOOR, STONE).contains(getElementAt(targetPoint.shiftTop()))
        ) {
            Direction randDirection = randomDirectionFrom(new Direction[]{RIGHT, LEFT});
            log.debug("Trap: ^ Escape: " + randDirection.name());
            return randDirection;
        }

        /**
         *  **
         * X@*
         *  **
         *  Escapes: ^ v
         */
        if (asList(WALL, START_FLOOR, STONE).contains(getElementAt(targetPoint.shiftTop()))
                && asList(WALL, START_FLOOR, STONE).contains(getElementAt(targetPoint.shiftBottom()))
                && asList(WALL, START_FLOOR, STONE).contains(getElementAt(targetPoint.shiftRight()))
        ) {
            Direction randDirection = randomDirectionFrom(new Direction[]{UP, DOWN});
            log.debug("Trap: > Escape: " + randDirection.name());
            return randDirection;
        }

        /**
         *  **
         *  *@X
         *  **
         *  Escapes: ^ v
         */
        if (asList(WALL, START_FLOOR, STONE).contains(getElementAt(targetPoint.shiftLeft()))
                && asList(WALL, START_FLOOR, STONE).contains(getElementAt(targetPoint.shiftBottom()))
                && asList(WALL, START_FLOOR, STONE).contains(getElementAt(targetPoint.shiftTop()))
        ) {
            Direction randDirection = randomDirectionFrom(new Direction[]{UP, DOWN});
            log.debug("Trap: < Escape: " + randDirection.name());
            return randDirection;
        }

        /**
         *  X
         * *@*
         * ***
         *
         * Escapes: <>
         */
        if (asList(WALL, START_FLOOR, STONE).contains(getElementAt(targetPoint.shiftLeft()))
                && asList(WALL, START_FLOOR, STONE).contains(getElementAt(targetPoint.shiftBottom()))
                && asList(WALL, START_FLOOR, STONE).contains(getElementAt(targetPoint.shiftRight()))
        ) {
            Direction randDirection = randomDirectionFrom(new Direction[]{RIGHT, LEFT});
            log.debug("Trap: v Escape: " + randDirection.name());
            return randDirection;
        }

        log.debug("No trap Go: " + direction.name());
        return direction;
    }

    /**
     * is neighbor point (next by direction) is acceptable to move on
     *
     * @param direction
     * @return
     */
    public boolean isAcceptable(Direction direction) {
        Map<Direction, BoardPoint> dirToPoints = neighborDirectionToPoints();
        BoardPoint point = dirToPoints.get(direction);
        return isAcceptable(point);
    }

    private Map<Direction, BoardPoint> neighborDirectionToPoints() {
        BoardPoint myHead = getMyHead();
        BoardPoint leftPoint = myHead.shiftLeft();
        BoardPoint rightPoint = myHead.shiftRight();
        BoardPoint upPoint = myHead.shiftTop();
        BoardPoint downPoint = myHead.shiftBottom();

        Map<Direction, BoardPoint> dirToPoints = new HashMap<>() {{
            put(UP, upPoint);
            put(DOWN, downPoint);
            put(RIGHT, rightPoint);
            put(LEFT, leftPoint);
        }};

        return dirToPoints;
    }

    public boolean isAcceptable(BoardPoint point) {
//        Map<Direction, BoardPoint> directionBoardPointMap = neighborDirectionToPoints();
        return !isTrap(point) && !isBadPoint(point) && !isYourBody(point);
    }

    private boolean isYourBody(BoardPoint point) {
        if (asList(BODY_HORIZONTAL, BODY_VERTICAL, BODY_LEFT_DOWN, BODY_LEFT_UP, BODY_RIGHT_DOWN, BODY_RIGHT_UP).contains(getElementAt(point))) {
            return true;
        }
        return false;
    }

    public boolean isTrap(BoardPoint targetPoint) {
        if (targetPoint.isOutOfBoard(this.size())) {
            return true;
        }
        if (trapIsUp(targetPoint) || trapIsRight(targetPoint) || trapIsLeft(targetPoint) || trapIsDown(targetPoint)) {
            return true;
        }
        return false;
    }

    public boolean isBadPoint(BoardPoint point) {
        if (point.isOutOfBoard(this.size())) {
            return true;
        }
        return !(GOODS_WITH_NONE).contains(getElementAt(point));
    }

    /**
     * ***
     * *@*
     * X
     *
     * Escapes: <>
     */
    private boolean trapIsUp(BoardPoint targetPoint) {
        if (targetPoint.getY() < 1)
            return false;
        return asList(WALL, START_FLOOR, STONE).contains(getElementAt(targetPoint.shiftRight()))
                && asList(WALL, START_FLOOR, STONE).contains(getElementAt(targetPoint.shiftLeft()))
                && asList(WALL, START_FLOOR, STONE).contains(getElementAt(targetPoint.shiftTop()));
    }

    /**
     * **
     * X@*
     * **
     * Escapes: ^ v
     */
    private boolean trapIsRight(BoardPoint targetPoint) {
        if (targetPoint.getX() >= this.size() - 1)
            return false;
        return asList(WALL, START_FLOOR, STONE).contains(getElementAt(targetPoint.shiftTop()))
                && asList(WALL, START_FLOOR, STONE).contains(getElementAt(targetPoint.shiftBottom()))
                && asList(WALL, START_FLOOR, STONE).contains(getElementAt(targetPoint.shiftRight()));
    }

    /**
     * **
     * *@X
     * **
     * Escapes: ^ v
     */
    private boolean trapIsLeft(BoardPoint targetPoint) {
        if (targetPoint.getX() < 1)
            return false;
        return asList(WALL, START_FLOOR, STONE).contains(getElementAt(targetPoint.shiftLeft()))
                && asList(WALL, START_FLOOR, STONE).contains(getElementAt(targetPoint.shiftBottom()))
                && asList(WALL, START_FLOOR, STONE).contains(getElementAt(targetPoint.shiftTop()));
    }

    /**
     * X
     * *@*
     * ***
     *
     * Escapes: <>
     */
    private boolean trapIsDown(BoardPoint targetPoint) {
        if (targetPoint.getY() >= this.size() - 1)
            return false;
        return asList(WALL, START_FLOOR, STONE).contains(getElementAt(targetPoint.shiftLeft()))
                && asList(WALL, START_FLOOR, STONE).contains(getElementAt(targetPoint.shiftBottom()))
                && asList(WALL, START_FLOOR, STONE).contains(getElementAt(targetPoint.shiftRight()));
    }

    /**
     * Есть ли на соседней клетке какой-нибудь ништяк
     * - есть => возвращаем направление
     * - нет => null
     */
    public Direction searchAnyGoodNeighborDirection() {
        for (BoardElement element : GOODS) {
            Direction direction = searchNeighborElementDirection(element);
            if (direction != null) {
                return direction;
            }
        }
        return null;
    }

    /**
     * Если на соседней клетке есть заданный элемент, то идем в данном направлении
     * Может быть больше одного вариант на соседних клетках, поэтому выбирается случайное
     *
     * @param element
     * @return
     */
    public Direction searchNeighborElementDirection(BoardElement element) {
        BoardPoint myHead = getMyHead();
        BoardPoint leftPoint = myHead.shiftLeft();
        BoardPoint rightPoint = myHead.shiftRight();
        BoardPoint upPoint = myHead.shiftTop();
        BoardPoint downPoint = myHead.shiftBottom();

        Map<BoardPoint, Direction> dirMap;

        int randInt = randomIntFor(6);
        switch (randInt) {
            case 1:
                dirMap = new HashMap<>() {{
                    putAll(Map.of(leftPoint, LEFT, rightPoint, RIGHT, upPoint, UP, downPoint, DOWN));
                }};
                break;
            case 2:
                dirMap = new HashMap<>() {{
                    putAll(Map.of(leftPoint, LEFT, upPoint, UP, rightPoint, RIGHT, downPoint, DOWN));
                }};
                break;
            case 3:
                dirMap = new HashMap<>() {{
                    putAll(Map.of(rightPoint, RIGHT, downPoint, DOWN, upPoint, UP, leftPoint, LEFT));
                }};
                break;
            case 4:
                dirMap = new HashMap<>() {{
                    putAll(Map.of(rightPoint, RIGHT, upPoint, UP, leftPoint, LEFT, downPoint, DOWN));
                }};
                break;
            case 5:
                dirMap = new HashMap<>() {{
                    putAll(Map.of(downPoint, DOWN, leftPoint, LEFT, upPoint, UP, rightPoint, RIGHT));
                }};
                break;
            case 6:
                dirMap = new HashMap<>() {{
                    putAll(Map.of(upPoint, UP, leftPoint, LEFT, downPoint, DOWN, rightPoint, RIGHT));
                }};
                break;
            default:
                dirMap = new HashMap<>() {{
                    putAll(Map.of(leftPoint, LEFT, rightPoint, RIGHT, upPoint, UP, downPoint, DOWN));
                }};
                break;
        }

        for (Map.Entry<BoardPoint, Direction> entry : dirMap.entrySet()) {
            BoardPoint neighborPoint = entry.getKey();
            Direction direction = entry.getValue();
            if (searchNeighborElementDirectionBy(neighborPoint, element)) {
                return direction;
            }
        }
        return null;
    }

    public boolean searchNeighborElementDirectionBy(BoardPoint neighborPoint, BoardElement requiredElement) {
        if (!neighborPoint.isOutOfBoard(this.size())) {
            BoardElement neighborElement = this.getElementAt(neighborPoint);
            if (requiredElement.equals(neighborElement)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Ищет по всему полю ништяки и возвращает направление первого шага до ништяка
     *
     * @param curDirection
     * @return
     */
    public Direction searchNearestGoodStepDir(Direction curDirection) {
        BoardPoint myHead = getMyHead();
        // к каким ништякам идем:
        // GOODS - тогда к любому ближайшему ништяку;
        // GOODS_WITH_POINTS - если стратегия только на заработку очков
        List<BoardPoint> goodPoints = findAllNearestElements(myHead, GOODS_WITH_POINTS);
        for (BoardPoint point : goodPoints) {
            if (isAcceptable(point)) {
                // ищем направление до этой точки
                return searchFirstStepDirection(myHead, point, curDirection);
            }
        }

        return null;
    }

    /**
     * Какое направление выбрать для первого шага к предмету
     *
     * Не может ходить в обратную сторону, поэтому, например,
     * если голова смотрит вправо, чтобы пойти влево, нужно сделать разворот через верх/низ
     */
    private Direction searchFirstStepDirection(BoardPoint from, BoardPoint to, Direction curDirection) {
        if (to.getX() > from.getX()) {
            if (curDirection != null && !curDirection.equals(LEFT)) {
                return RIGHT;
            } else {
                return randomDirectionFrom(new Direction[]{UP, DOWN});
            }
        } else if (to.getX() < from.getX()) {
            if (curDirection != null && !curDirection.equals(RIGHT)) {
                return LEFT;
            } else {
                return randomDirectionFrom(new Direction[]{UP, DOWN});
            }
        }

        if (to.getY() > from.getY()) {
            if (curDirection != null && !curDirection.equals(UP)) {
                return DOWN;
            } else {
                return randomDirectionFrom(new Direction[]{RIGHT, LEFT});
            }
        } else if (to.getY() < from.getY()) {
            if (curDirection != null && !curDirection.equals(DOWN)) {
                return UP;
            } else {
                return randomDirectionFrom(new Direction[]{RIGHT, LEFT});
            }
        }

        return null;
    }

//    public Direction lookForGoods() {
//        List<Direction> directions = asList(LEFT, DOWN, UP, DOWN);
//        for (Direction direction : directions) {
//            Direction trueDirection = lookForwardForGoods(direction, getMyHead());
//            if (trueDirection != null) {
//                return trueDirection;
//            }
//        }
//        return null;
//    }

//    /**
//     * @param direction
//     * @param myHead
//     * @return
//     */
//    public Direction lookForwardForGoods(Direction direction, BoardPoint myHead) {
//        switch (direction) {
//            case LEFT:
//                BoardPoint leftPoint = myHead.shiftLeft();
//                BoardElement leftElement = getElementAt(leftPoint);
//                while (NONE.equals(leftElement) && !leftPoint.isOutOfBoard(size())) {
//                    lookForwardForGoods(LEFT, leftPoint);
//                }
//                return GOODS.contains(leftElement) ? LEFT : null;
//            case RIGHT:
//                BoardPoint rightPoint = myHead.shiftRight();
//                BoardElement rightElement = getElementAt(rightPoint);
//                while (NONE.equals(rightElement) && !rightPoint.isOutOfBoard(size())) {
//                    lookForwardForGoods(RIGHT, rightPoint);
//                }
//                return GOODS.contains(rightElement) ? RIGHT : null;
//            case UP:
//                BoardPoint upPoint = myHead.shiftTop();
//                BoardElement upElement = getElementAt(upPoint);
//                while (NONE.equals(upElement) && !upPoint.isOutOfBoard(size())) {
//                    lookForwardForGoods(UP, upPoint);
//                }
//                return GOODS.contains(upElement) ? UP : null;
//            case DOWN:
//                BoardPoint downPoint = myHead.shiftTop();
//                BoardElement downElement = getElementAt(downPoint);
//                while (NONE.equals(downElement) && !downPoint.isOutOfBoard(size())) {
//                    lookForwardForGoods(DOWN, downPoint);
//                }
//                return GOODS.contains(downElement) ? DOWN : null;
//            default:
//                return null;
//        }
//    }

    private Direction randomDirectionFrom(Direction[] directions) {
        var random = new Random(System.currentTimeMillis());
        Direction dir = directions[random.nextInt(directions.length)];
        return dir;
    }

    private int randomIntFor(int num) {
        var random = new Random(System.currentTimeMillis());
        return random.nextInt(num);
    }
}
