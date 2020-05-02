set(protobuf_source_dir "${CMAKE_CURRENT_LIST_DIR}/protobuf")
set(protobuf_SHARED_OR_STATIC STATIC)
set(protobuf_VERSION "1.0.0")

include(${CMAKE_CURRENT_LIST_DIR}/protobuf/cmake/libprotobuf-lite.cmake)
include(${CMAKE_CURRENT_LIST_DIR}/protobuf/cmake/libprotobuf.cmake)
target_link_libraries(libprotobuf PRIVATE log)
target_compile_definitions(libprotobuf PRIVATE HAVE_PTHREAD)
add_executable(protobuf::protoc IMPORTED)
set_target_properties(protobuf::protoc PROPERTIES IMPORTED_LOCATION "${CMAKE_CURRENT_LIST_DIR}/protoc/protoc")

set(Protobuf_LIBRARIES libprotobuf)
set(Protobuf_INCLUDE_DIRS )