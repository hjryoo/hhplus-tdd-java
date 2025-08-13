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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @InjectMocks
    private PointService pointService;

    private static final long FIXED_TIME = 1634567890000L;

    /* 사용자 포인트 조회 동작 검증 */
    @Test
    @DisplayName("사용자 ID로 포인트를 조회한다")
    void givenUserId_whenGetUserPoint_thenReturnsUserPoint() {
        // 특정 사용자의 ID
        long userId = 1L;
        UserPoint expectedPoint = new UserPoint(userId, 1000L, FIXED_TIME);
        given(userPointTable.selectById(userId)).willReturn(expectedPoint);
        // 포인트 조회할 떄
        UserPoint result = pointService.getUserPoint(userId);

        // 해당 사용자의 포인트를 반환한다
        assertThat(result).isEqualTo(expectedPoint);
        assertThat(result.point()).isEqualTo(1000L);
    }

    /* 사용자별 포인트 내역 조회 검증 */
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

    /* 신규 사용자나 거래 내역이 없는 경우에도 시스템이 안정적으로 빈 리스트를 반환하는지 검증 */
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

    /* 정상적인 포인트 충전 로직과 잔액 계산의 정확성을 검증 */
    @Test
    @DisplayName("유효한 금액으로 포인트를 충전한다")
    void givenValidAmount_whenChargePoint_thenReturnsUpdatedPoint() {
        // 현재 500원 보유한 사용자가 1000원을 충전하는 상황
        long userId = 1L;
        long currentAmount = 500L;
        long chargeAmount = 1000L;
        long expectedAmount = 1500L; // 500 + 1000 = 1500

        UserPoint currentPoint = new UserPoint(userId, currentAmount, FIXED_TIME);
        UserPoint expectedPoint = new UserPoint(userId, expectedAmount, FIXED_TIME);

        given(userPointTable.selectById(userId)).willReturn(currentPoint);
        given(userPointTable.insertOrUpdate(userId, expectedAmount)).willReturn(expectedPoint);

        // 포인트 충전 실행
        UserPoint result = pointService.chargePoint(userId, chargeAmount);

        // 결과 검증
        assertThat(result.point()).isEqualTo(expectedAmount); // 최종 포인트 확인
        // 충전 내역 저장 여부 검증
        verify(pointHistoryTable, times(1)).insert(anyLong(), anyLong(), eq(TransactionType.CHARGE), anyLong());
    }

    /* 음수 금액 충전 시도 시 적절한 예외 처리와 데이터 무결성 보장 검증 */
    @Test
    @DisplayName("음수 금액으로 충전할 수 없다")
    void givenNegativeAmount_whenChargePoint_thenThrowsException() {

        // 음수 충전 금액으로 시도
        long userId = 1L;
        long negativeAmount = -1000L; // 음수 금액

        // 예외 발생 검증
        assertThatThrownBy(() -> pointService.chargePoint(userId, negativeAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전 금액은 0보다 커야 합니다.");

        verifyNoInteractions(userPointTable, pointHistoryTable);
    }

    /* 정상적인 포인트 사용 로직과 잔액 차감의 정확성 검증 */
    @Test
    @DisplayName("잔고가 충분할 때 포인트를 사용한다")
    void givenSufficientBalance_whenUsePoint_thenReturnsUpdatedPoint() {

        // 잔고 1500원, 사용하려는 금액 1000원
        long userId = 1L;
        long currentAmount = 1500L;
        long useAmount = 1000L;
        long expectedAmount = 500L; // 1500 - 1000 = 500

        UserPoint currentPoint = new UserPoint(userId, currentAmount, FIXED_TIME);
        UserPoint expectedPoint = new UserPoint(userId, expectedAmount, FIXED_TIME);

        given(userPointTable.selectById(userId)).willReturn(currentPoint);
        given(userPointTable.insertOrUpdate(userId, expectedAmount)).willReturn(expectedPoint);

        // 포인트 사용
        UserPoint result = pointService.usePoint(userId, useAmount);

        // 차감된 포인트 잔액이 정확한지 검증
        assertThat(result.point()).isEqualTo(expectedAmount);
        // 사용 내역이 정확히 1회 기록되었는지 검증
        verify(pointHistoryTable, times(1)).insert(anyLong(), anyLong(), eq(TransactionType.USE), anyLong());
    }

    /* "잔고 부족 시 포인트 사용 실패" 비즈니스 규칙 검증 */
    @Test
    @DisplayName("잔고가 부족하면 포인트 사용이 실패한다")
    void givenInsufficientBalance_whenUsePoint_thenThrowsException() {
        // given: 잔고 500원, 사용하려는 금액 1000원
        long userId = 1L;
        long currentAmount = 500L;
        long useAmount = 1000L; // 잔고보다 큰 금액

        UserPoint currentPoint = new UserPoint(userId, currentAmount, FIXED_TIME);
        given(userPointTable.selectById(userId)).willReturn(currentPoint);

        // 예외 발생 및 메시지 검증
        assertThatThrownBy(() -> pointService.usePoint(userId, useAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잔고가 부족합니다.");

        // 데이터 무결성 보장
        // 포인트 업데이트가 발생하지 않았는지 검증
        verify(userPointTable, never()).insertOrUpdate(anyLong(), anyLong());
        // 잘못된 사용 내역이 기록되지 않았는지 검증
        verify(pointHistoryTable, never()).insert(anyLong(), anyLong(), any(TransactionType.class), anyLong());
    }

}
