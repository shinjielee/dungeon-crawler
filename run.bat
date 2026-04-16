@echo off
chcp 65001 >nul
echo ==========================================
echo   地牢獵人 - Dungeon Crawler Auto-Battler
echo ==========================================

if not exist out mkdir out

echo [1/2] 正在編譯...
javac -encoding UTF-8 -sourcepath src -d out src\Main.java
if %errorlevel% neq 0 (
    echo.
    echo [錯誤] 編譯失敗！請確認已安裝 JDK 17 或以上版本。
    echo 可從 https://adoptium.net/ 下載安裝。
    pause
    exit /b 1
)

echo [2/2] 啟動遊戲...
java -cp out -Dfile.encoding=UTF-8 Main
