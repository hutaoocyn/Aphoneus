@rem Gradle startup script for Windows
@echo off
setlocal
set CLASSPATH=%~dp0gradle\wrapper\gradle-wrapper.jar
java -jar "%CLASSPATH%" %*
