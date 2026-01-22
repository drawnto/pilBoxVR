// 17jan26 Software Lab. Alexander Burger

#include <errno.h>
#include <string.h>
#include <unistd.h>
#include <aarch64-linux-android/asm/unistd_64.h>
#include <sys/types.h>
#include <sys/mman.h>
#include <sys/socket.h>
#include <sys/un.h>

// Not available in Android's bionic libc
static inline int memfd_create(const char *name, unsigned int flags) {
   return syscall(__NR_memfd_create, name, flags);
}

#include <jni.h>
#include <android/native_window.h>
#include <android/hardware_buffer.h>
#include <android/native_window_jni.h>
#include <android/sharedmem.h>

static ANativeWindow *Window;

static int Size;
static int ShmFd = -1;
static void *Pixmap = MAP_FAILED;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
   return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL Java_de_software_1lab_pilbox_FbView_fbWindow(JNIEnv *env, jobject thiz, jobject surface) {
   ANativeWindow_Buffer buf;

   if (surface) {
      Window = ANativeWindow_fromSurface(env, surface);
      if (ANativeWindow_setBuffersGeometry(Window, 0, 0, AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM) != 0)
         Window = NULL;
      else if (Pixmap != MAP_FAILED  &&  ANativeWindow_lock(Window, &buf, NULL) >= 0) {
         memcpy(buf.bits, Pixmap, Size);
         ANativeWindow_unlockAndPost(Window);
      }
   }
   else if (Window) {
      ANativeWindow_release(Window);
      Window = NULL;
   }
}

JNIEXPORT int JNICALL Java_de_software_1lab_pilbox_PicoLisp_fbBeg(JNIEnv *env, jobject thiz, int size) {
   int res;

   if ((res = ShmFd = memfd_create("pilBoxFB", 0)) >= 0) {
      if (ftruncate(ShmFd, Size = size) < 0  ||  (Pixmap = mmap(NULL, size, PROT_READ, MAP_SHARED, ShmFd, 0)) == MAP_FAILED)
         close(ShmFd),  ShmFd = -1,  res = -2;
      else {
         struct sockaddr_un addr;
         memset(&addr, 0, sizeof(addr));
         addr.sun_family = AF_UNIX;
         strcpy(addr.sun_path, "/data/data/de.software_lab.pilbox/FbSock");

         struct iovec iov = {NULL, 0};

         char cmsgbuf[CMSG_SPACE(sizeof(int))];
         memset(cmsgbuf, 0, sizeof(cmsgbuf));

         struct msghdr msg;
         memset(&msg, 0, sizeof(msg));
         msg.msg_name = &addr;
         msg.msg_namelen = sizeof(addr);
         msg.msg_iov = &iov;
         msg.msg_iovlen = 1;
         msg.msg_control = cmsgbuf;
         msg.msg_controllen = sizeof(cmsgbuf);

         struct cmsghdr *cmsg = CMSG_FIRSTHDR(&msg);
         cmsg->cmsg_level = SOL_SOCKET;
         cmsg->cmsg_type = SCM_RIGHTS;
         cmsg->cmsg_len = CMSG_LEN(sizeof(int));
         *(int*)CMSG_DATA(cmsg) = ShmFd;

         int sock;
         if ((sock = socket(AF_UNIX, SOCK_DGRAM, 0)) < 0)
            res = -3;
         else {
            if ((res = sendmsg(sock, &msg, 0)) < 0)
               res = -4;
            close(sock);
         }
      }
   }
   return res < 0? res * -1000 + errno : 0;
}

JNIEXPORT void JNICALL Java_de_software_1lab_pilbox_PicoLisp_fbDraw(void) {
   ANativeWindow_Buffer buf;

   if (Window  &&  Pixmap != MAP_FAILED  &&  ANativeWindow_lock(Window, &buf, NULL) >= 0) {
      memcpy(buf.bits, Pixmap, Size);
      ANativeWindow_unlockAndPost(Window);
   }
}

JNIEXPORT void JNICALL Java_de_software_1lab_pilbox_PicoLisp_fbEnd(void) {
   if (ShmFd >= 0) {
      if (Pixmap != MAP_FAILED) {
         munmap(Pixmap, Size);
         Pixmap = MAP_FAILED;
      }
      close(ShmFd);
      ShmFd = -1;
   }
}
