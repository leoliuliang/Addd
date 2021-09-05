#设置一些变量，rtmp默认加密的，这里设置主要为了禁止加密
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DNO_CRYPTO")

#设置一个全局file变量，变量名时rtmp_source ；*.c代表将目录下所有.c结尾的文件都放到rtmp_source变量下
file(GLOB rtmp_source *.c)

#把librtmp路径下的所有.c结尾的文件编译为static静态库，库名为rtmp
add_library(rtmp
        STATIC
        ${rtmp_source})