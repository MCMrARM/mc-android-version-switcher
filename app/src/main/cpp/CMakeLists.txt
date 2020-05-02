cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_MODULE_PATH "${CMAKE_SOURCE_DIR}/../../../../deps/modules" ${CMAKE_MODULE_PATH})

set(CROSS_COMPILE_ARSG "-G${CMAKE_GENERATOR}")
set(CROSS_COMPILE_CACHE_ARGS "-DCMAKE_TOOLCHAIN_FILE:FILEPATH=${CMAKE_TOOLCHAIN_FILE}"
        "-DANDROID_ABI:STRING=${ANDROID_ABI}" "-DANDROID_PLATFORM:STRING=${ANDROID_PLATFORM}"
        "-DANDROID_NDK:PATH=${ANDROID_NDK}" "-DCMAKE_MAKE_PROGRAM:FILEPATH=${CMAKE_MAKE_PROGRAM}")

include(../../../../deps/boringssl.cmake)
include(../../../../deps/curl.cmake)
include(../../../../deps/libuv.cmake)
include(../../../../deps/protobuf.cmake)
add_subdirectory(../../../../deps/google-play-api google-play-api)

add_library(native-lib SHARED native-lib.cpp ../../../../deps/google-play-api/src/config.cpp)

target_link_libraries(native-lib gplayapi)