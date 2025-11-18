# --- Optional Argument ---
file ?=

# Phony targets are targets that do not correspond to an actual file.
.PHONY: build run help lint lint-fix e2e test

# Default target
all: build

# --- Local Targets ---

build:
	@echo "📦 Building standalone JAR..."
	@mvn -U package assembly:single
	@mkdir -p ./release
	@mv ./target/gecko-1.0-jar-with-dependencies.jar ./release/
	@echo "✅ JAR successfully built to ./release/gecko-1.0-jar-with-dependencies.jar"

run:
	@java -Xmx3G --add-modules jdk.compiler -Dpolyglot.js.nashorn-compat=true -jar ./release/gecko-1.0-jar-with-dependencies.jar $(file)

lint:
	@echo "🔎 Checking code formatting..."
	@mvn fmt:check
	@echo "✅ Code is formatted correctly."

lint-fix:
	@echo "✨ Automatically reformatting code..."
	@mvn fmt:format
	@echo "✅ Code reformatting complete."

test:
	@echo "🧪 Running unit tests..."
	@mvn test
	@echo "✅ Unit tests passed."

help:
	@echo "Usage: make <target> [file=<path>]"
	@echo ""
	@echo "Examples:"
	@echo "  make run"
	@echo "  make run file=my_circuit_projects/VSI.ipes"
	@echo ""
	@echo "Targets:"
	@echo "  build    : Builds the runnable JAR to ./release/ (runs tests)."
	@echo "  run      : Runs the application. Optionally pass 'file=<path>'."
	@echo "  test     : Runs the Java unit tests."
	@echo "  lint     : Checks if the Java code is formatted correctly."
	@echo "  lint-fix : Automatically reformats all Java source files."