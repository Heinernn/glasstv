# Paso de parche de OpenSSL: el scrub original y, en anfitrion Windows, el arreglo de rutas de 15-android.conf.
include(${CMAKE_CURRENT_LIST_DIR}/scrub_mkbuildinf.cmake)

if(CMAKE_HOST_SYSTEM_NAME STREQUAL "Windows" AND FIX_PY)
    execute_process(
        COMMAND ${FIX_PY} ${CMAKE_CURRENT_LIST_DIR}/fix_android_conf_windows.py
        RESULT_VARIABLE _fix_result
    )
    if(NOT _fix_result EQUAL 0)
        message(FATAL_ERROR "fix_android_conf_windows.py failed (${_fix_result})")
    endif()
endif()
