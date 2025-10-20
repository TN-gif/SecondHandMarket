@echo off
chcp 65001 > nul
echo ========================================
echo Running Unit Tests
echo ========================================
echo.

REM Compile source code if not already compiled
echo [1/3] Compiling source code...
javac -encoding UTF-8 -cp "lib/*" -d out/production/SecondHandMarket -sourcepath src src/Main.java src/InitialDataSetup.java 2>nul
if errorlevel 1 (
    echo [ERROR] Source compilation failed!
    pause
    exit /b 1
)
echo [SUCCESS] Source code compiled.
echo.

REM Compile tests
echo [2/3] Compiling tests...
javac -encoding UTF-8 -cp "lib/*;out/production/SecondHandMarket" -d out/test test/service/*.java 2>nul
if errorlevel 1 (
    echo [ERROR] Test compilation failed!
    pause
    exit /b 1
)
echo [SUCCESS] Tests compiled.
echo.

REM Run tests
echo [3/3] Running tests...
echo.
echo ----------------------------------------
echo UserService Tests
echo ----------------------------------------
java -cp "out/test;out/production/SecondHandMarket;lib/*" service.UserServiceTest
echo.

echo ----------------------------------------
echo ProductService Tests
echo ----------------------------------------
java -cp "out/test;out/production/SecondHandMarket;lib/*" service.ProductServiceTest
echo.

echo ----------------------------------------
echo OrderService Tests
echo ----------------------------------------
java -cp "out/test;out/production/SecondHandMarket;lib/*" service.OrderServiceTest
echo.

echo ----------------------------------------
echo ReviewService Tests
echo ----------------------------------------
java -cp "out/test;out/production/SecondHandMarket;lib/*" service.ReviewServiceTest
echo.

echo ========================================
echo All Tests Completed
echo ========================================
pause

