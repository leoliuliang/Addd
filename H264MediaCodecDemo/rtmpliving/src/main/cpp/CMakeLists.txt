
cmake_minimum_required(VERSION 3.10.2)
#添加一个子文件
add_subdirectory(librtmp)

add_library( rtmp-lib
             SHARED
        rtmp-lib.cpp)
find_library(
              log-lib
              log )

target_link_libraries(
        rtmp-lib
                       ${log-lib}
rtmp)

