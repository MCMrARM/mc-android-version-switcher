include(ExternalProject)

set(CURL_OPTIONS "-DCMAKE_INSTALL_PREFIX:PATH=${CMAKE_BINARY_DIR}/ext/curl"
        "-DBUILD_CURL_EXE:BOOL=OFF"
        "-DBUILD_SHARED_LIBS:BOOL=OFF"
        "-DCURL_STATICLIB:BOOL=ON"
        "-DCURL_DISABLE_LDAP:BOOL=ON"
        "-DCMAKE_USE_LIBSSH2:BOOL=OFF"
        "-DCMAKE_USE_OPENLDAP:BOOL=OFF"
        "-DBUILD_TESTING:BOOL=OFF"
        "-DCMAKE_MODULE_PATH:PATH=${CMAKE_MODULE_PATH}"
        "-DOPENSSL_INCLUDE_DIR:PATH=${OPENSSL_INCLUDE_DIR}"
        "-DOPENSSL_SSL_LIBRARY:PATH=${OPENSSL_SSL_LIBRARY}"
        "-DOPENSSL_CRYPTO_LIBRARY:PATH=${OPENSSL_CRYPTO_LIBRARY}"
        "-DHAVE_POLL_FINE_EXITCODE:TEXT=1")

set(CURL_STATIC_LIBRARIES ${CMAKE_BINARY_DIR}/ext/curl/lib/libcurl.a)

ExternalProject_Add(
        curl_ext
        PREFIX curl
        URL "http://curl.haxx.se/download/curl-7.63.0.tar.gz"
        BUILD_BYPRODUCTS ${CURL_STATIC_LIBRARIES}
        INSTALL_DIR ${CMAKE_BINARY_DIR}/ext/curl
        CMAKE_ARGS ${CROSS_COMPILE_ARSG}
        CMAKE_CACHE_ARGS ${CURL_OPTIONS} ${CROSS_COMPILE_CACHE_ARGS}
)
add_dependencies(curl_ext boringssl)
file(MAKE_DIRECTORY ${CMAKE_BINARY_DIR}/ext/curl/include/)
add_library(curl STATIC IMPORTED)
add_dependencies(curl curl_ext)
set_property(TARGET curl PROPERTY IMPORTED_LOCATION ${CMAKE_BINARY_DIR}/ext/curl/lib/libcurl.a)
set_property(TARGET curl PROPERTY INTERFACE_INCLUDE_DIRECTORIES ${CMAKE_BINARY_DIR}/ext/curl/include/)
set_property(TARGET curl PROPERTY INTERFACE_LINK_LIBRARIES boringssl z)

set(CURL_FOUND TRUE)
set(CURL_LIBRARY curl)
set(CURL_LIBRARIES curl)
set(CURL_INCLUDE_DIR ${CMAKE_BINARY_DIR}/ext/curl/include/)
set(CURL_INCLUDE_DIRS ${CMAKE_BINARY_DIR}/ext/curl/include/)

