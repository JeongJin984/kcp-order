package kcp.common.exception;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OutOfStockException extends BusinessException {

  public OutOfStockException(int requiredCount, int actualCount) {
    super(ErrorCode.STOCK_SHORTAGE, "stock shortage (request : {} / current {})", requiredCount, actualCount);
  }
}
