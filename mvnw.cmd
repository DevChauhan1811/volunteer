@echo off
set "MAVEN_BIN=C:\Users\Dev Chauhan\maven\apache-maven-3.9.9\bin\mvn.cmd"
if exist "%MAVEN_BIN%" (
    "%MAVEN_BIN%" %*
) else (
    mvn %*
)
