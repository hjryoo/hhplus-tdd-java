package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @InjectMocks
    private PointService pointService;

    private static final long FIXED_TIME = 1634567890000L;

    @Test
    @DisplayName("사용자 ID로 포인트를 조회한다")
    void givenUserId_whenGetUserPoint_thenReturnsUserPoint() {
        // 특정 사용자의 ID로
        long userId = 1L;
        UserPoint expectedPoint = new UserPoint(userId, 1000L, FIXED_TIME);
        given(userPointTable.selectById(userId)).willReturn(expectedPoint);
        // 포인트 조회할 떄
        UserPoint result = pointService.getUserPoint(userId);

        // 해당 사용자의 포인트를 반환한다
        assertThat(result).isEqualTo(expectedPoint);
        assertThat(result.point()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("사용자 ID로 포인트 충전/사용 내역을 조회한다")
    void givenUserId_whenGetPointHistories_thenReturnsHistories() {
        // 특정 사용자의 포인트 내역이 존재하는 상황
        long userId = 1L;
        List<PointHistory> expectedHistories = List.of(
                new PointHistory(1L, userId, 1000L, TransactionType.CHARGE, FIXED_TIME),
                new PointHistory(2L, userId, 500L, TransactionType.USE, FIXED_TIME + 1000),
                new PointHistory(3L, userId, 2000L, TransactionType.CHARGE, FIXED_TIME + 2000)
        );
        given(pointHistoryTable.selectAllByUserId(userId)).willReturn(expectedHistories);

        // 포인트 내역을 조회할 때
        List<PointHistory> result = pointService.getPointHistories(userId);

        // 해당 사용자의 모든 포인트 내역이 반환된다
        assertThat(result).hasSize(3);
        assertThat(result).isEqualTo(expectedHistories);

        // 각 내역의 상세 검증
        assertThat(result.get(0).type()).isEqualTo(TransactionType.CHARGE);
        assertThat(result.get(0).amount()).isEqualTo(1000L);
        assertThat(result.get(1).type()).isEqualTo(TransactionType.USE);
        assertThat(result.get(1).amount()).isEqualTo(500L);
        assertThat(result.get(2).type()).isEqualTo(TransactionType.CHARGE);
        assertThat(result.get(2).amount()).isEqualTo(2000L);
    }

    @Test
    @DisplayName("내역이 없는 사용자의 경우 빈 리스트를 반환한다")
    void givenUserWithNoHistory_whenGetPointHistories_thenReturnsEmptyList() {
        // 포인트 내역이 없는 사용자
        long userId = 999L;
        given(pointHistoryTable.selectAllByUserId(userId)).willReturn(List.of());

        // 포인트 내역을 조회할 때
        List<PointHistory> result = pointService.getPointHistories(userId);

        // 빈 리스트가 반환된다
        assertThat(result).isEmpty();
        assertThat(result).hasSize(0);
    }

}
