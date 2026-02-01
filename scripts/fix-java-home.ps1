# JAVA_HOME 수정 스크립트 (현재 사용 중인 JDK 경로로 설정)
# 실제 Java 위치: C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot

$newJavaHome = "C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot"

if (-not (Test-Path $newJavaHome)) {
    Write-Host "오류: JDK 경로가 없습니다. $newJavaHome" -ForegroundColor Red
    Write-Host "다른 JDK가 설치되어 있다면 위 경로를 수정한 뒤 다시 실행하세요." -ForegroundColor Yellow
    exit 1
}

# 1) 현재 세션에만 적용 (바로 이 터미널에서 gradle 등 실행할 때)
$env:JAVA_HOME = $newJavaHome
Write-Host "[현재 세션] JAVA_HOME = $newJavaHome" -ForegroundColor Green

# 2) 사용자 환경 변수에 영구 저장 (다음부터 새 터미널에서도 유효)
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", $newJavaHome, "User")
Write-Host "[영구 설정] 사용자 환경 변수 JAVA_HOME 저장됨." -ForegroundColor Green

Write-Host ""
Write-Host "적용 완료. 이 터미널에서 gradlew 등을 다시 실행해 보세요." -ForegroundColor Cyan
Write-Host "새로 연 터미널에서는 자동으로 적용됩니다." -ForegroundColor Cyan
