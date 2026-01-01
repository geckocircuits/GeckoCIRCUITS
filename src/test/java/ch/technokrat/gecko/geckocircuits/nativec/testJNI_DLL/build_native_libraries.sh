#!/bin/bash
# Build script for native JNI test libraries
# This script compiles the testJNI_DLL.c file for the current platform

# Detect operating system
OS="$(uname -s)"
case "${OS}" in
    Linux*)     PLATFORM=Linux;;
    Darwin*)    PLATFORM=Mac;;
    MINGW*|MSYS*|CYGWIN*)  PLATFORM=Windows;;
    *)          PLATFORM="UNKNOWN:${OS}"
esac

echo "Detected platform: $PLATFORM"

# Get Java include directories
if [ -z "$JAVA_HOME" ]; then
    echo "Error: JAVA_HOME environment variable not set"
    exit 1
fi

JAVA_INCLUDE="$JAVA_HOME/include"
if [ "$PLATFORM" = "Mac" ]; then
    JAVA_PLATFORM_INCLUDE="$JAVA_HOME/include/darwin"
elif [ "$PLATFORM" = "Linux" ]; then
    JAVA_PLATFORM_INCLUDE="$JAVA_HOME/include/linux"
elif [ "$PLATFORM" = "Windows" ]; then
    JAVA_PLATFORM_INCLUDE="$JAVA_HOME/include/win32"
fi

echo "Java include directory: $JAVA_INCLUDE"

# Compile based on platform
case "${PLATFORM}" in
    Linux)
        echo "Building Linux shared library (.so)..."
        gcc -shared -fPIC -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" \
            -o libtestJNI_DLL.so testJNI_DLL.c
        cp libtestJNI_DLL.so libtestJNI_DLL2.so
        echo "Built: libtestJNI_DLL.so and libtestJNI_DLL2.so"
        ;;

    Mac)
        echo "Building macOS dynamic library (.dylib)..."
        gcc -shared -fPIC -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/darwin" \
            -o libtestJNI_DLL.dylib testJNI_DLL.c
        cp libtestJNI_DLL.dylib libtestJNI_DLL2.dylib
        echo "Built: libtestJNI_DLL.dylib and libtestJNI_DLL2.dylib"
        ;;

    Windows)
        echo "Building Windows DLL (.dll)..."
        gcc -shared -fPIC -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/win32" \
            -o libtestJNI_DLL.dll testJNI_DLL.c -Wl,--kill-at
        cp libtestJNI_DLL.dll libtestJNI_DLL2.dll
        echo "Built: libtestJNI_DLL.dll and libtestJNI_DLL2.dll"
        ;;

    *)
        echo "Error: Unsupported platform: $PLATFORM"
        exit 1
        ;;
esac

echo "Build complete!"
