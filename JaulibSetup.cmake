#
# jaulib cmake build settings, modularized to be optionally included by parent projects
#

include_guard(GLOBAL)

macro(JaulibSetup)

message(STATUS "JaulibSetup: ${PROJECT_NAME}")

set(ENV{LANG} en_US.UTF-8)
set(ENV{LC_MEASUREMENT} en_US.UTF-8)

# for all
set (CMAKE_CXX_STANDARD 17)
set (CMAKE_CXX_STANDARD_REQUIRED ON)

# for all
set (CC_FLAGS_WARNING "-Wall -Wextra -Werror")
set (GCC_FLAGS_WARNING_FORMAT "-Wformat=2 -Wformat-overflow=2 -Wformat-nonliteral -Wformat-security -Wformat-signedness -Wformat-y2k")
set (GCC_FLAGS_WARNING "-Wall -Wextra -Wshadow -Wtype-limits -Wsign-compare -Wcast-align=strict -Wnull-dereference -Winit-self ${GCC_FLAGS_WARNING_FORMAT} -Werror")
# causes issues in jau::get_int8(..): "-Wnull-dereference"
set (GCC_FLAGS_WARNING_NO_ERROR "-Wno-error=array-bounds -Wno-error=null-dereference -Wno-multichar")

# too pedantic, but nice to check once in a while
# set (DISABLED_CC_FLAGS_WARNING "-Wsign-conversion")

# debug only
set (GCC_FLAGS_STACK "-fstack-protector-strong -fstack-check")
set (GCC_FLAGS_SANITIZE_ALLLEAK "-fsanitize-address-use-after-scope -fsanitize=address -fsanitize=pointer-compare -fsanitize=pointer-subtract -fsanitize=undefined -fsanitize=leak")
set (GCC_FLAGS_SANITIZE_UNDEFINED "-fsanitize=undefined")
set (GCC_FLAGS_SANITIZE_THREAD "-fsanitize-address-use-after-scope -fsanitize=undefined -fsanitize=thread")
# -fsanitize=address cannot be combined with -fsanitize=thread
# -fsanitize=pointer-compare -fsanitize=pointer-subtract must be combined with -fsanitize=address
# -fsanitize=thread TSAN's lacks ability to properly handle GCC's atomic macros (like helgrind etc), can't check SC-DRF!

if(CMAKE_COMPILER_IS_GNUCC)
    # shorten __FILE__ string and the like ..
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${GCC_FLAGS_WARNING} ${GCC_FLAGS_WARNING_NO_ERROR} -fmacro-prefix-map=${CMAKE_SOURCE_DIR}/=/")
else()
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${CC_FLAGS_WARNING}")
endif(CMAKE_COMPILER_IS_GNUCC)

message(STATUS "${PROJECT_NAME} USE_STRIP = ${USE_STRIP} (pre-set)")

if(DEBUG)
    if(CMAKE_COMPILER_IS_GNUCC)
        set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -g -ggdb -DDEBUG -fno-omit-frame-pointer ${GCC_FLAGS_STACK} -no-pie")
    else()
        set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -g -DDEBUG")
    endif(CMAKE_COMPILER_IS_GNUCC)
    if(INSTRUMENTATION)
        if(CMAKE_COMPILER_IS_GNUCC)
            set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${GCC_FLAGS_SANITIZE_ALLLEAK}")
        endif(CMAKE_COMPILER_IS_GNUCC)
    elseif(INSTRUMENTATION_UNDEFINED)
        if(CMAKE_COMPILER_IS_GNUCC)
            set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${GCC_FLAGS_SANITIZE_UNDEFINED}")
        endif(CMAKE_COMPILER_IS_GNUCC)
    elseif(INSTRUMENTATION_THREAD)
        if(CMAKE_COMPILER_IS_GNUCC)
            set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${GCC_FLAGS_SANITIZE_THREAD}")
        endif(CMAKE_COMPILER_IS_GNUCC)
    endif(INSTRUMENTATION)
elseif(GPROF)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -g -ggdb -pg -fno-rtti")
elseif(PERF_ANALYSIS)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -g -ggdb -fno-rtti")
else()
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -fno-rtti")
    find_program(STRIP strip)
    if (STRIP STREQUAL "STRIP-NOTFOUND")
        set(USE_STRIP OFF)
        message(STATUS "${PROJECT_NAME} USE_STRIP:=false, strip not found")
    elseif(NOT DEFINED USE_STRIP)
        set(USE_STRIP ON)
        message(STATUS "${PROJECT_NAME} USE_STRIP:=true, !DEBUG and not set")
    endif()
endif(DEBUG)

message(STATUS "${PROJECT_NAME} USE_STRIP = ${USE_STRIP} (now)")

# '-latomic' is required using gcc-8 on Raspberry ... to avoid: undefined reference to `__atomic_store_8'
if(DEBUG)
    if(CMAKE_COMPILER_IS_GNUCC)
        set(CMAKE_SHARED_LINKER_FLAGS  "${CMAKE_SHARED_LINKER_FLAGS} -latomic -no-pie")
        set(CMAKE_EXE_LINKER_FLAGS  "${CMAKE_EXE_LINKER_FLAGS} -latomic -no-pie")
    else()
        set(CMAKE_SHARED_LINKER_FLAGS  "${CMAKE_SHARED_LINKER_FLAGS} -latomic")
        set(CMAKE_EXE_LINKER_FLAGS  "${CMAKE_EXE_LINKER_FLAGS} -latomic")
    endif(CMAKE_COMPILER_IS_GNUCC)
else()
    set(CMAKE_SHARED_LINKER_FLAGS  "${CMAKE_SHARED_LINKER_FLAGS} -fno-rtti -latomic")
    set(CMAKE_EXE_LINKER_FLAGS  "${CMAKE_EXE_LINKER_FLAGS} -fno-rtti -latomic")
endif(DEBUG)


set (LIB_INSTALL_DIR "lib${LIB_SUFFIX}" CACHE PATH "Installation path for libraries")

# Set CMAKE_INSTALL_XXXDIR (XXX {BIN LIB ..} if not defined
# (was: CMAKE_LIB_INSTALL_DIR)
include(GNUInstallDirs)

# Appends the cmake/modules path to MAKE_MODULE_PATH variable.
set (CMAKE_MODULE_PATH ${CMAKE_CURRENT_SOURCE_DIR}/cmake/modules ${CMAKE_MODULE_PATH})

# Determine OS_AND_ARCH as library appendix, e.g. 'direct_bt-linux-amd64'
string(TOLOWER ${CMAKE_SYSTEM_NAME} OS_NAME)
if(${CMAKE_SYSTEM_PROCESSOR} STREQUAL "arm")
    set(OS_ARCH "armhf")
elseif(${CMAKE_SYSTEM_PROCESSOR} STREQUAL "armv7l")
    set(OS_ARCH "armhf")
elseif(${CMAKE_SYSTEM_PROCESSOR} STREQUAL "aarch64")
    set(OS_ARCH "arm64")
elseif(${CMAKE_SYSTEM_PROCESSOR} STREQUAL "x86_64")
    set(OS_ARCH "amd64")
else()
    set(OS_ARCH ${CMAKE_SYSTEM_PROCESSOR})
endif()
set(OS_AND_ARCH ${OS_NAME}-${OS_ARCH})
set(os_and_arch_slash ${OS_NAME}/${OS_ARCH})
set(os_and_arch_dot ${OS_NAME}.${OS_ARCH})

message (STATUS "OS_NAME ${OS_NAME}")
message (STATUS "OS_ARCH ${OS_ARCH} (${CMAKE_SYSTEM_PROCESSOR})")
message (STATUS "OS_AND_ARCH ${OS_AND_ARCH}")

# Make a version file containing the current version from git.
include (GetGitRevisionDescription)

git_describe (VERSION "--tags")
get_git_head_revision(GIT_REFSPEC VERSION_SHA1 ALLOW_LOOKING_ABOVE_CMAKE_SOURCE_DIR)
git_local_changes(GIT_WORKDIR_STATUS)
message(STATUS "${PROJECT_NAME} git_describe ${VERSION}")
message(STATUS "${PROJECT_NAME} get_git_head_revision ${GIT_REFSPEC}")
message(STATUS "${PROJECT_NAME} get_git_head_revision ${VERSION_SHA1}")
message(STATUS "${PROJECT_NAME} git_local_changes ${GIT_WORKDIR_STATUS}")

if ("x_${VERSION}" STREQUAL "x_GIT-NOTFOUND" OR "x_${VERSION}" STREQUAL "x_HEAD-HASH-NOTFOUND" OR "x_${VERSION}" STREQUAL "x_-128-NOTFOUND")
  message (WARNING " - Install git to compile for production!")
  set (VERSION "v1.0.0-dirty")
endif ()

message (STATUS "${PROJECT_NAME} Version ${VERSION}")

#parse the version information into pieces.
string (REGEX REPLACE "^v([0-9]+)\\..*" "\\1" VERSION_MAJOR "${VERSION}")
string (REGEX REPLACE "^v[0-9]+\\.([0-9]+).*" "\\1" VERSION_MINOR "${VERSION}")
string (REGEX REPLACE "^v[0-9]+\\.[0-9]+\\.([0-9]+).*" "\\1" VERSION_PATCH "${VERSION}")
string (REGEX REPLACE "^v[0-9]+\\.[0-9]+\\.[0-9]+\\-([0-9]+).*" "\\1" VERSION_COMMIT "${VERSION}")
#string (REGEX REPLACE "^v[0-9]+\\.[0-9]+\\.[0-9]+-[0-9]+\\-(.*)" "\\1" VERSION_SHA1 "${VERSION}")
set (VERSION_SHORT "${VERSION_MAJOR}.${VERSION_MINOR}.${VERSION_PATCH}")
set (VERSION_API "${VERSION_MAJOR}.${VERSION_MINOR}")
string(TIMESTAMP BUILD_TSTAMP "%Y-%m-%d %H:%M:%S")

if ("${VERSION_COMMIT}" MATCHES "^v.*")
  set (VERSION_COMMIT "")
endif()

IF(BUILDJAVA)
    find_package(Java 11 REQUIRED)
    find_package(JNI REQUIRED)
    include(UseJava)

    if (JNI_FOUND)
        message (STATUS "JNI_INCLUDE_DIRS=${JNI_INCLUDE_DIRS}")
        message (STATUS "JNI_LIBRARIES=${JNI_LIBRARIES}")
    endif (JNI_FOUND)

    if (NOT DEFINED $ENV{JAVA_HOME_NATIVE})
      set (JAVA_HOME_NATIVE $ENV{JAVA_HOME})
      set (JAVAC $ENV{JAVA_HOME}/bin/javac)
      set (JAR $ENV{JAVA_HOME}/bin/jar)
    else ()
      set (JAVAC $ENV{JAVA_HOME_NATIVE}/bin/javac)
      set (JAR $ENV{JAVA_HOME_NATIVE}/bin/jar)
    endif ()

    set(CMAKE_JAVA_COMPILE_FLAGS ${CMAKE_JAVA_COMPILE_FLAGS} -source 11 -target 11 -encoding UTF-8)
    if(DEBUG)
        if(DEFINED JAVAC_DEBUG_ARGS)
            set(CMAKE_JAVA_COMPILE_FLAGS ${CMAKE_JAVA_COMPILE_FLAGS} -g:${JAVAC_DEBUG_ARGS})
        else()
            set(CMAKE_JAVA_COMPILE_FLAGS ${CMAKE_JAVA_COMPILE_FLAGS} -g:source,lines)
        endif()
    else()
        # Adding source,lines (default javac debug setting) adds ~13% or 30k.
        # jaulib_fat.jar: No-Debug: 237458 bytes, Def-Debug: 267221 bytes (source, lines)
        if(DEFINED JAVAC_DEBUG_ARGS)
            set(CMAKE_JAVA_COMPILE_FLAGS ${CMAKE_JAVA_COMPILE_FLAGS} -g:${JAVAC_DEBUG_ARGS})
        else()
            set(CMAKE_JAVA_COMPILE_FLAGS ${CMAKE_JAVA_COMPILE_FLAGS} -g:source,lines)
            # set(CMAKE_JAVA_COMPILE_FLAGS ${CMAKE_JAVA_COMPILE_FLAGS} -g:none)
        endif()
    endif(DEBUG)
    message(STATUS "${PROJECT_NAME} JAVAC_FLAGS = ${CMAKE_JAVA_COMPILE_FLAGS}")
ENDIF(BUILDJAVA)

endmacro()

