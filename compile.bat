@echo off
chcp 65001 >nul
echo ========================================
echo 编译校园二手商品交易管理系统
echo ========================================
echo.

REM 创建输出目录
if not exist "out\production\SecondHandMarket" (
    mkdir "out\production\SecondHandMarket"
)

REM 编译项目
echo [编译] 正在编译所有Java文件...
javac -encoding UTF-8 -cp "lib/*" -d out/production/SecondHandMarket -sourcepath src src/Main.java src/InitialDataSetup.java

if errorlevel 1 (
    echo.
    echo [错误] 编译失败！请检查错误信息。
    pause
    exit /b 1
)

echo.
echo [完成] 编译成功！
echo.
echo 提示：运行 run.bat 启动系统
echo.

pause


