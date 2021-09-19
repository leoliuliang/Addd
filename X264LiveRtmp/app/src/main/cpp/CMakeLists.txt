
cmake_minimum_required(VERSION 3.10.2)

project("x264livertmp")
add_subdirectory(librtmp)


message(${CMAKE_SOURCE_DIR})
message(${ANDROID_ABI})



file(GLOB cpp_source *.cpp)

#导入头文件
include_directories(${CMAKE_SOURCE_DIR}/x264/${ANDROID_ABI}/include)
include_directories(${CMAKE_SOURCE_DIR}/faac/${ANDROID_ABI}/include)
#导入静态库，设置静态库环境变量方便导入
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/x264/${ANDROID_ABI}/lib")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/faac/${ANDROID_ABI}/lib")



add_library( # Sets the name of the library.
             native-lib

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             ${cpp_source})


find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )


target_link_libraries( # Specifies the target library.
                       native-lib

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib}
                        rtmp
                        x264
                        faac)