@echo off
chcp 65001 >nul
set "JAVA_HOME=C:\Program Files\Android\Android Studio2\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "GRADLE_OPTS=-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"
gradlew.bat compileDebugKotlin compileDebugUnitTestKotlin --no-parallel --console=plain --no-daemon