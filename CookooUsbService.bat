@echo off
rem 设置工程路程和apk名

rem D1Launcher
set project_dir=D:\kobeMananger\workplace\source\git\platform\hmi_media_scan\MainUI\UsbMedia
set input_apk_name=CookooUsbService
set output_apk_name=CookooUsbService

rem 下面的不做改动

java -jar D:\SIGNAPK\signapk.jar D:\SIGNAPK\platform.x509.pem D:\SIGNAPK\platform.pk8 %project_dir%\bin\%input_apk_name%.apk D:\SIGNAPK\out\%output_apk_name%.apk
