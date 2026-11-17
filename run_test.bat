@echo off
chcp 65001 >nul
set "JAVA_HOME=C:\Program Files\Android\Android Studio2\jbr"
cd /d C:\Users\Public\Projects\TxtNotes
gradlew.bat testDebugUnitTest --tests "ru.whiteleaf.notes.common.utils.FileUtilsTest" --no-parallel --no-daemon --console=plain
