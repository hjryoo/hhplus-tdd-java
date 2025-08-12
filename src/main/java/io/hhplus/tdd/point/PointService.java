package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PointService {
    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    public PointService(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    /**
     * 특정 사용자의 포인트를 조회한다.
     *
     * @param userId 사용자 ID
     * @return 사용자의 현재 포인트 정보
     */
    public UserPoint getUserPoint(long userId) {
        return userPointTable.selectById(userId);
    }

    /**
     * 특정 사용자의 포인트 충전/사용 내역을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 포인트 내역 리스트
     */
    public List<PointHistory> getPointHistories(long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }

    /**
     * 특정 사용자의 포인트를 충전한다.
     *
     * @param userId 사용자 ID
     * @param amount 충전할 금액
     * @return 충전 후 포인트 정보
     * @throws IllegalArgumentException 충전 금액이 0 이하인 경우
     */
    public UserPoint chargePoint(long userId, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }

        // 현재 포인트 조회
        UserPoint currentPoint = userPointTable.selectById(userId);

        // 새로운 포인트 계산
        long newAmount = currentPoint.point() + amount;

        // 포인트 업데이트
        UserPoint updatedPoint = userPointTable.insertOrUpdate(userId, newAmount);

        // 충전 내역 저장
        pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());

        return updatedPoint;
    }

    /**
     * 특정 사용자의 포인트를 사용한다.
     *
     * @param userId 사용자 ID
     * @param amount 사용할 금액
     * @return 사용 후 포인트 정보
     * @throws IllegalArgumentException 사용 금액이 0 이하이거나 잔고가 부족한 경우
     */
    public UserPoint usePoint(long userId, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("사용 금액은 0보다 커야 합니다.");
        }

        // 현재 포인트 조회
        UserPoint currentPoint = userPointTable.selectById(userId);

        // 잔고 부족 검증
        if (currentPoint.point() < amount) {
            throw new IllegalArgumentException("잔고가 부족합니다.");
        }

        // 새로운 포인트 계산
        long newAmount = currentPoint.point() - amount;

        // 포인트 업데이트
        UserPoint updatedPoint = userPointTable.insertOrUpdate(userId, newAmount);

        // 사용 내역 저장
        pointHistoryTable.insert(userId, amount, TransactionType.USE, System.currentTimeMillis());

        return updatedPoint;
    }

}
