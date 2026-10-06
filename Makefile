# Native build: needs a JDK 17+ (Android Studio's bundled JBR works) and the Android SDK.
# Point ANDROID_HOME at the SDK (or create local.properties with sdk.dir=...).
ANDROID_HOME ?= $(HOME)/Library/Android/sdk
export ANDROID_HOME
ADB := $(ANDROID_HOME)/platform-tools/adb
APP_ID := com.andrecoura.homemonitor

# Fall back to Android Studio's JDK on macOS when JAVA_HOME is not set.
ifeq ($(JAVA_HOME),)
  STUDIO_JBR := /Applications/Android Studio.app/Contents/jbr/Contents/Home
  ifneq ($(shell test -d "$(STUDIO_JBR)" && echo y),)
    export JAVA_HOME := $(STUDIO_JBR)
  endif
endif

GRADLE := ./gradlew

.PHONY: help build test lint format coverage instrumented install run clean

help:
	@echo "build        assemble the debug APK"
	@echo "test         unit tests (JVM, no emulator)"
	@echo "lint         Android Lint + ktlint (warnings are errors)"
	@echo "format       auto-format Kotlin with ktlint"
	@echo "coverage     unit tests + Kover report + 80% gate + per-layer table"
	@echo "instrumented UI tests on a running emulator/device"
	@echo "install      install the debug APK on the running emulator/device"
	@echo "run          install and launch"
	@echo "clean        remove build outputs"

build:
	$(GRADLE) :app:assembleDebug

test:
	$(GRADLE) :app:testDebugUnitTest

lint:
	$(GRADLE) ktlintCheck :app:lintDebug

format:
	$(GRADLE) ktlintFormat

coverage:
	$(GRADLE) :app:koverVerify :app:koverHtmlReport :app:koverXmlReport
	@python3 scripts/coverage-summary.py
	@echo "HTML report: app/build/reports/kover/html/index.html"

instrumented:
	$(GRADLE) :app:connectedDebugAndroidTest

install:
	$(GRADLE) :app:installDebug

run: install
	$(ADB) shell am start -n $(APP_ID)/.MainActivity

clean:
	$(GRADLE) clean
