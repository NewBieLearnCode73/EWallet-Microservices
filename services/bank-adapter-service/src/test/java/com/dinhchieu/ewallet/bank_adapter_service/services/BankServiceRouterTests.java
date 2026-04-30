package com.dinhchieu.ewallet.bank_adapter_service.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dinhchieu.ewallet.bank_adapter_service.dtos.response.BankResult;
import com.dinhchieu.ewallet.bank_adapter_service.enums.BankCodeError;
import com.dinhchieu.ewallet.bank_adapter_service.enums.BankStatus;

@ExtendWith(MockitoExtension.class)
public class BankServiceRouterTests {

  private BankServiceRouter router;
  @Mock
  private BankStrategy vcbStrategy;
  @Mock
  private BankStrategy tcbStrategy;

  @BeforeEach
  void setUp() {
    when(vcbStrategy.getBankCode()).thenReturn("VCB");
    when(tcbStrategy.getBankCode()).thenReturn("TCB");
    List<BankStrategy> strategies = List.of(vcbStrategy, tcbStrategy);
    router = new BankServiceRouter(strategies);
  }

  @Test
  void processDeposit_ShouldCallCorrectStrategy_WhenBankCodeIsValid() {
    // Arrange
    String sagaId = "123";
    String bankCode = "TCB";
    double amount = 1000.0;
    String account = "999999";

    BankResult expectedResult = BankResult.builder().status("SUCCESS").build();

    when(tcbStrategy.processDeposit(sagaId, amount, account))
        .thenReturn(expectedResult);

    // Act
    BankResult result = router.processDeposit(sagaId, bankCode, amount, account);

    // Assert
    Assertions.assertThat(result).isEqualTo(expectedResult);
    verify(tcbStrategy, times(1)).processDeposit(sagaId, amount, account);
    verify(vcbStrategy, times(0)).processDeposit(sagaId, amount, account);
  }

  @Test
  void processDeposit_ShouldReturnFailure_WhenBankCodeIsUnknown() {
    // Arrange
    String sagaId = "123";
    String bankCode = "UNKNOWN_BANK";
    double amount = 100.0;
    String account = "111";

    // Act
    BankResult result = router.processDeposit(sagaId, bankCode, amount, account);

    // Assert
    Assertions.assertThat(result).isNotNull();
    Assertions.assertThat(result.getStatus()).isEqualTo(BankStatus.FAILURE.name());
    Assertions.assertThat(result.getErrorCode()).isEqualTo(BankCodeError.BANK_CODE_NOT_SUPPORTED.getCode());
    Assertions.assertThat(result.getErrorMessage()).isEqualTo(BankCodeError.BANK_CODE_NOT_SUPPORTED.getMessage());

    verify(tcbStrategy, never()).processDeposit(any(), anyDouble(), any());
    verify(vcbStrategy, never()).processDeposit(any(), anyDouble(), any());
  }

  @Test
  void processWithdrawal_ShouldCallCorrectStrategy() {
    // Arrange
    String bankCode = "TCB";

    // Act
    router.processWithdrawal("Saga-001", bankCode, 500.0, "ACC-TCB");

    verify(tcbStrategy).processWithdrawal("Saga-001", 500.0, "ACC-TCB");
    verify(vcbStrategy, never()).processWithdrawal(any(), anyDouble(), any());
  }
}