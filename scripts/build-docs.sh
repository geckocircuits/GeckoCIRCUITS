#!/bin/bash
# Build and optionally deploy GeckoCIRCUITS documentation
# Usage: ./scripts/build-docs.sh [serve|build|deploy]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_ROOT"

# Check if pip/python is available
if ! command -v python3 &> /dev/null; then
    echo "Error: Python 3 is required but not installed."
    exit 1
fi

# Install dependencies if needed
if ! python3 -c "import mkdocs" 2>/dev/null; then
    echo "Installing MkDocs dependencies..."
    pip3 install -r docs/requirements.txt
fi

# Parse command
CMD="${1:-serve}"

case "$CMD" in
    serve)
        echo "Starting MkDocs development server..."
        echo "Open http://127.0.0.1:8000 in your browser"
        mkdocs serve
        ;;

    build)
        echo "Building documentation site..."
        mkdocs build --clean
        echo "Site built in: site/"
        ;;

    deploy)
        echo "Deploying to GitHub Pages..."
        mkdocs gh-deploy --force
        echo "Documentation deployed to GitHub Pages!"
        ;;

    sync)
        echo "Syncing resources/tutorials to docs/tutorials..."
        # This would copy/convert markdown files from resources to docs
        python3 scripts/sync-docs.py
        ;;

    *)
        echo "Usage: $0 [serve|build|deploy|sync]"
        echo ""
        echo "Commands:"
        echo "  serve   - Start local development server (default)"
        echo "  build   - Build static site to site/"
        echo "  deploy  - Deploy to GitHub Pages"
        echo "  sync    - Sync resources/tutorials to docs/"
        exit 1
        ;;
esac
