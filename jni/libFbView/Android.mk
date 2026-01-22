# 05jan26 Software Lab. Alexander Burger

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := libFbView
LOCAL_SRC_FILES := FbView.c
LOCAL_C_INCLUDES := $(LOCAL_PATH)
include $(BUILD_SHARED_LIBRARY)

# .SILENT:

CC = clang
SHARED = -shared
STRIP = strip

LIB = ../bin/lib/arm64-v8a

#all: $(LIB)/libFbView.so

$(LIB)/libFbView.so: FbView.c
	$(CC) -o $(LIB)/libFbView.so $(SHARED) FbView.c ../bin/lib/arm64-v8a/libandroid.so ../bin/lib/arm64-v8a/libnativewindow.so
	$(STRIP) $(LIB)/libFbView.so

clean:
	rm $(LIB)/libFbView.so
