#!/bin/bash

# Danh sách 7 services (Đã bỏ config-server, Eureka lên làm Đại ca)
SERVICES=(
  "platforms/eureka-server"
  "services/api-gateway"
  "services/auth-service"
  "services/profile-service"
  "services/bank-adapter-service"
  "services/wallet-service"
  "services/transaction-service"
)

# Thư mục gốc của project
BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# ĐƯỜNG DẪN CONFIG (Mấu chốt để bỏ Config Server)
# Trỏ thẳng vào folder config-repo trong project của bạn
export CONFIG_PATH="$BASE_DIR/config/config-repo"

# Kỹ thuật ép cân RAM: Max 256MB cho mỗi service
JAVA_OPTS="-Xms128m -Xmx256m"

# Spring Profile: dev hoặc docker
SPRING_PROFILE="${1:-dev}"

# Tên thư mục chứa log
LOG_DIR="$BASE_DIR/logs"

# Tạo thư mục logs nếu chưa tồn tại
if [ ! -d "$LOG_DIR" ]; then
  mkdir -p "$LOG_DIR"
  echo "[INFO] Đã tạo thư mục $LOG_DIR để gom log."
fi

PIDS=()

cleanup() {
  echo -e "\n[INFO] Đang tắt toàn bộ hệ thống..."
  for pid in "${PIDS[@]}"; do
    if kill -0 "$pid" 2>/dev/null; then
      kill "$pid"
      echo "  -> Đã tắt tiến trình PID: $pid"
    fi
  done
  echo "[INFO] Đã dọn dẹp xong! Máy tính của bạn đã được cứu!"
  exit 0
}

trap cleanup SIGINT

echo "======================================================="
echo " KHỞI ĐỘNG E-WALLET (BỎ CONFIG SERVER - DÙNG FILE LOCAL)"
echo " CONFIG PATH: $CONFIG_PATH"
echo " SPRING PROFILE: $SPRING_PROFILE"
echo "======================================================="

for SERVICE_PATH in "${SERVICES[@]}"; do
  SERVICE_NAME="$(basename "$SERVICE_PATH")"

  # Tìm file jar
  JAR_FILE=$(find "$BASE_DIR/$SERVICE_PATH/build/libs" -name "*.jar" ! -name "*-plain.jar" | head -n 1)

  if [ -z "$JAR_FILE" ]; then
    echo "[ERROR] Không tìm thấy file .jar của $SERVICE_NAME. Build lại đi Chiêu!"
  else
    echo "[STARTING] Đang gọi $SERVICE_NAME thức dậy..."
    
    LOG_FILE="$LOG_DIR/$SERVICE_NAME.log"
    
    # CHỖ THAY ĐỔI: Truyền CONFIG_PATH vào lệnh chạy java
    # Dùng -D để chắc chắn Spring nhận diện được biến hệ thống
    # Thêm spring.profiles.active để chỉ định profile (dev hoặc docker)
    java $JAVA_OPTS -DCONFIG_PATH="$CONFIG_PATH" -jar "$JAR_FILE" --spring.profiles.active="$SPRING_PROFILE" > "$LOG_FILE" 2>&1 &
    
    PID=$!
    PIDS+=($PID)
    echo "  -> Đang chạy (PID: $PID). Xem log: tail -f $LOG_FILE"
    
    # Chờ Eureka khởi động xong mới chạy các service khác
    if [ "$SERVICE_NAME" == "eureka-server" ]; then
       echo "  -> Chờ 12s cho Eureka Server ổn định..."
       sleep 12
    fi
  fi
done

echo "======================================================="
echo "[SUCCESS] Hệ thống đã lên nòng!"
echo "[INFO] Bấm 'Ctrl + C' để TẮT TOÀN BỘ."
echo "======================================================="

wait