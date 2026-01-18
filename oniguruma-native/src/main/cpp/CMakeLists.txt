cmake_minimum_required(VERSION 3.22.1)

project("oniguruma-binding")

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_FLAGS "-fvisibility=hidden")

set(BUILD_TEST OFF CACHE BOOL "Disable oniguruma tests" FORCE)
add_subdirectory(oniguruma EXCLUDE_FROM_ALL)

add_library("oniguruma-binding" SHARED binding.cpp)

target_link_libraries("oniguruma-binding" PUBLIC onig)
