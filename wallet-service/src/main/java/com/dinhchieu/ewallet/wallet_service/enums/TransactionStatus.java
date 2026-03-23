package com.dinhchieu.ewallet.wallet_service.enums;

public enum TransactionStatus {
  COMPLETED, // Transaction hoàn thành thành công
  ROLLED_BACK // Transaction bị rollback (do compensation saga)
}
