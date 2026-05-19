@echo off
rem Run the Gane project with LWJGL 2 native libraries
setlocal

set PROJECT_DIR=%~dp0
set BIN=%PROJECT_DIR%bin
set LIB=%PROJECT_DIR%lwjgl-2.9.3\jar
set NATIVES=%PROJECT_DIR%lwjgl-2.9.3\native\windows

set CP=%BIN%;%LIB%\lwjgl.jar;%LIB%\lwjgl_util.jar;%LIB%\lwjgl_util_applet.jar;%LIB%\jinput.jar;%LIB%\lwjgl_test.jar;%LIB%\lwjgl-debug.jar;%LIB%\asm-debug-all.jar;%PROJECT_DIR%lib\pngdecoder.jar

java -Djava.library.path="%NATIVES%" -cp "%CP%" gane.Main

endlocal
