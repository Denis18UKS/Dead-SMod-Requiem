@echo off
setlocal
if not exist original.jar (
  echo ERROR: original.jar is missing.
  exit /b 1
)
if not exist build mkdir build
if not exist build\classes mkdir build\classes
if not exist build\patcher-classes mkdir build\patcher-classes
javac --release 8 -encoding UTF-8 -d build\classes src\main\java\net\mcreator\horrormod\unified\*.java tests\stubs\net\minecraft\nbt\*.java tests\stubs\net\minecraft\world\storage\*.java
if errorlevel 1 exit /b 1
javac --add-exports java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED --add-exports java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED -d build\patcher-classes tools\PatchJar.java
if errorlevel 1 exit /b 1
java --add-exports java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED --add-exports java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED -cp build\patcher-classes PatchJar original.jar build\classes build\DeadsMod-Requiem-Forge-1.16.5.jar
if errorlevel 1 exit /b 1
echo Build complete: build\DeadsMod-Requiem-Forge-1.16.5.jar
