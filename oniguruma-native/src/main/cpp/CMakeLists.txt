cmake_minimum_required(VERSION 3.22.1)

project("oniguruma-binding")

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_FLAGS "-fvisibility=hidden")

add_subdirectory(oniguruma)

add_library("oniguruma-binding" SHARED binding.cpp)

target_link_libraries("oniguruma-binding" PUBLIC onig)
target_link_options(${CMAKE_PROJECT_NAME} PRIVATE "-Wl,-z,max-page-size=16384")
