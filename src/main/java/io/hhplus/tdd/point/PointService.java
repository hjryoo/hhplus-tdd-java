package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.springframework.stereotype.Service;

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

}
