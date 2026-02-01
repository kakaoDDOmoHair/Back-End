#!/usr/bin/env bash
# JAVA_HOME 수정 (Git Bash / MINGW64용)
# 실제 Java: C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot

JAVA_HOME_VALUE="C:\\Program Files\\Microsoft\\jdk-17.0.17.10-hotspot"

# 1) 현재 세션에 적용
export JAVA_HOME="$JAVA_HOME_VALUE"
echo "[현재 세션] JAVA_HOME = $JAVA_HOME"

# 2) Windows 사용자 환경 변수에 영구 저장 (setx)
if command -v setx &>/dev/null; then
  setx JAVA_HOME "$JAVA_HOME_VALUE" > /dev/null 2>&1
  echo "[영구 설정] 사용자 환경 변수 JAVA_HOME 저장됨."
fi

echo ""
echo "적용 완료. 이 터미널에서 gradlew 등을 다시 실행해 보세요."
echo "새로 연 터미널에서는 자동으로 적용됩니다."
