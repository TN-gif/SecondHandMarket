@echo off
chcp 65001 >nul
echo ========================================
echo 校园二手商品交易管理系统
echo ========================================
echo.

REM 检查是否已编译
if not exist "out\production\SecondHandMarket\Main.class" (
    echo [编译] 正在编译项目...
    javac -encoding UTF-8 -cp "lib/*" -d out/production/SecondHandMarket -sourcepath src src/Main.java src/InitialDataSetup.java
    if errorlevel 1 (
        echo [错误] 编译失败！
        pause
        exit /b 1
    )
    echo [完成] 编译成功！
    echo.
)

REM 运行程序
echo [启动] 正在启动系统...
echo.
java -cp "out/production/SecondHandMarket;lib/*" Main

pause


