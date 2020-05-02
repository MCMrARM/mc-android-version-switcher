# Copyright 2017 The TensorFlow Authors. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ==============================================================================
include (ExternalProject)

set(boringssl_INCLUDE_DIR ${CMAKE_CURRENT_BINARY_DIR}/boringssl/src/boringssl/include)
#set(boringssl_EXTRA_INCLUDE_DIR ${CMAKE_CURRENT_BINARY_DIR}/boringssl/src)
set(boringssl_URL https://boringssl.googlesource.com/boringssl)
set(boringssl_TAG eeb5bb3561347f121911781754cd464bf49e2c32)
set(boringssl_BUILD ${CMAKE_BINARY_DIR}/boringssl/src/boringssl-build)
#set(boringssl_LIBRARIES ${boringssl_BUILD}/obj/so/libboringssl.so)
set(boringssl_STATIC_LIBRARIES
        ${boringssl_BUILD}/ssl/libssl.a
        ${boringssl_BUILD}/crypto/libcrypto.a
        ${boringssl_BUILD}/decrepit/libdecrepit.a
        )
set(boringssl_EXTRA_OPTIONS )
if ("${CMAKE_SYSTEM_PROCESSOR}" MATCHES arm*)
    set(boringssl_EXTRA_OPTIONS "-DANDROID_ARM_MODE:TEXT=arm")
endif()

ExternalProject_Add(boringssl_ext
        PREFIX boringssl
        SOURCE_DIR ${CMAKE_BINARY_DIR}/boringssl/src/boringssl
        BINARY_DIR ${boringssl_BUILD}
        GIT_REPOSITORY ${boringssl_URL}
        GIT_TAG ${boringssl_TAG}
        DOWNLOAD_DIR "${DOWNLOAD_LOCATION}"
        # BUILD_IN_SOURCE 1
        BUILD_BYPRODUCTS ${boringssl_STATIC_LIBRARIES}
        INSTALL_COMMAND ""
        CMAKE_ARGS ${CROSS_COMPILE_ARSG}
        CMAKE_CACHE_ARGS
        -DCMAKE_POSITION_INDEPENDENT_CODE:BOOL=${tensorflow_ENABLE_POSITION_INDEPENDENT_CODE}
        -DCMAKE_BUILD_TYPE:STRING=Release
        -DCMAKE_VERBOSE_MAKEFILE:BOOL=OFF
        ${CROSS_COMPILE_CACHE_ARGS}
        ${boringssl_EXTRA_OPTIONS}
        )

file(MAKE_DIRECTORY ${boringssl_INCLUDE_DIR})

add_library(boringssl INTERFACE IMPORTED)
add_dependencies(boringssl boringssl_ext)
set_property(TARGET boringssl PROPERTY INTERFACE_INCLUDE_DIRECTORIES ${boringssl_INCLUDE_DIR})
set_property(TARGET boringssl PROPERTY INTERFACE_LINK_LIBRARIES ${boringssl_STATIC_LIBRARIES})

# define BoringSSL as OpenSSL for our purposes

set(OPENSSL_INCLUDE_DIR ${boringssl_INCLUDE_DIR})
set(OPENSSL_SSL_LIBRARY ${boringssl_BUILD}/ssl/libssl.a)
set(OPENSSL_CRYPTO_LIBRARY ${boringssl_BUILD}/crypto/libcrypto.a)
