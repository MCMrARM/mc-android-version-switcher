include(ExternalProject)

set(LIBUV_OPTIONS "-DCMAKE_INSTALL_PREFIX:PATH=${CMAKE_BINARY_DIR}/ext/libuv"
        "-DBUILD_SHARED_LIBS:BOOL=OFF"
        "-DBUILD_TESTING:BOOL=OFF"
        "-DCMAKE_MODULE_PATH:PATH=${CMAKE_MODULE_PATH}")

set(LIBUV_STATIC_LIBRARIES ${CMAKE_BINARY_DIR}/ext/libuv/lib/libuv_a.a)

ExternalProject_Add(
        libuv_ext
        PREFIX libuv
        URL "https://github.com/libuv/libuv/archive/v1.37.0.tar.gz"
        BUILD_BYPRODUCTS ${LIBUV_STATIC_LIBRARIES}
        INSTALL_DIR ${CMAKE_BINARY_DIR}/ext/libuv
        CMAKE_ARGS ${CROSS_COMPILE_ARSG}
        CMAKE_CACHE_ARGS ${LIBUV_OPTIONS} ${CROSS_COMPILE_CACHE_ARGS}
)
file(MAKE_DIRECTORY ${CMAKE_BINARY_DIR}/ext/libuv/include/)
add_library(libuv STATIC IMPORTED)
add_dependencies(libuv libuv_ext)
set_property(TARGET libuv PROPERTY IMPORTED_LOCATION ${CMAKE_BINARY_DIR}/ext/libuv/lib/libuv_a.a)
set_property(TARGET libuv PROPERTY INTERFACE_INCLUDE_DIRECTORIES ${CMAKE_BINARY_DIR}/ext/libuv/include/)

set(LIBUV_FOUND TRUE)
set(LIBUV_LIBRARIES libuv)
set(LIBUV_INCLUDE_DIRS ${CMAKE_BINARY_DIR}/ext/libuv/include/)

