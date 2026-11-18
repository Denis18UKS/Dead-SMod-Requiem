@echo off
setlocal
if not exist original.jar (
  echo ERROR: original.jar is missing. Place the original Dead's Mod client jar here.
  exit /b 1
)
python -m pip install --disable-pip-version-check pillow
if errorlevel 1 exit /b 1
python tools\generate_compass_assets.py
if errorlevel 1 exit /b 1
python tools\structure_mesh.py --build original.jar src\main\resources\assets\horrormod\requiem
if errorlevel 1 exit /b 1
if not exist build\classes mkdir build\classes
if not exist build\patcher-classes mkdir build\patcher-classes
(for /r src\main\java %%F in (*.java) do @echo "%%F") > build\source-files.txt
(for /r tests\stubs %%F in (*.java) do @echo "%%F") >> build\source-files.txt
javac --release 8 -encoding UTF-8 -d build\classes @build\source-files.txt
if errorlevel 1 exit /b 1
javac --add-exports java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED --add-exports java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED -d build\patcher-classes tools\PatchJar.java
if errorlevel 1 exit /b 1
java --add-exports java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED --add-exports java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED -cp build\patcher-classes PatchJar original.jar build\classes build\DeadsMod-Requiem-v3.3-Forge-1.16.5.jar
if errorlevel 1 exit /b 1
echo Build complete: build\DeadsMod-Requiem-v3.3-Forge-1.16.5.jar
