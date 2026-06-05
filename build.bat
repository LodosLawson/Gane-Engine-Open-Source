dir /s /b src\*.java Engine\*.java > sources.txt
javac -encoding UTF-8 -cp "lib/*;lib/json.jar;lwjgl-2.9.3/jar/*;src;Engine" --release 17 -d bin @sources.txt
del sources.txt
xcopy /s /y /i src\res\* bin\res\ > nul
xcopy /s /y /i Engine\res\* bin\res\ > nul
xcopy /s /y /i src\*.txt bin\ > nul
xcopy /s /y /i Engine\*.txt bin\ > nul
xcopy /s /y /i src\*.vert bin\ > nul
xcopy /s /y /i Engine\*.vert bin\ > nul
xcopy /s /y /i src\*.frag bin\ > nul
xcopy /s /y /i Engine\*.frag bin\ > nul
xcopy /s /y /i src\*.comp bin\ > nul
xcopy /s /y /i Engine\*.comp bin\ > nul
xcopy /s /y /i src\*.glsl bin\ > nul
xcopy /s /y /i Engine\*.glsl bin\ > nul

