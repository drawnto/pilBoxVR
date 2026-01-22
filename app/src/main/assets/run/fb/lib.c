// 28sep25 Software Lab. Alexander Burger
// https://wiki.osdev.org/Scalable_Screen_Font
// https://gitlab.com/bztsrc/scalable-font2/blob/master/docs/API.md

#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>

/* Normal renderer */
#define SSFN_IMPLEMENTATION
#include "ssfn.h"

ssfn_buf_t *ssfnInit(uint8_t *ptr, int width, int height) {
   ssfn_buf_t *buf;

   memset(ptr, 0, 4 * width * height);
   buf = malloc(sizeof(ssfn_buf_t));
   buf->ptr = ptr;
   buf->w = width;
   buf->h = height;
   buf->p = width * 4;
   buf->x = 0;
   buf->y = 0;
   buf->fg = 0xFFAAAAAA;
   buf->bg = 0;
   return buf;
}

ssfn_t *ssfnCtx(void) {
   ssfn_t *ctx;

   ctx = malloc(sizeof(ssfn_t));
   memset(ctx, 0, sizeof(ssfn_t));
   return ctx;
}

const char *ssfnFont(ssfn_t *ctx, ssfn_font_t *font) {
   int e;

   if (!font  ||  (e = ssfn_load(ctx, font)) == SSFN_OK)
      return NULL;
   return ssfn_error(e);
}

const char *ssfnSelect(ssfn_t *ctx, int size) {
   int e;

   if ((e = ssfn_select(ctx, SSFN_FAMILY_ANY, NULL, SSFN_STYLE_REGULAR, size)) == SSFN_OK)
      return NULL;
   return ssfn_error(e);
}

void ssfnColor(ssfn_buf_t *buf, uint32_t fg, uint32_t bg) {
   buf->fg = fg;
   buf->bg = bg;
}

void ssfnRender(ssfn_t *ctx, ssfn_buf_t *buf, int x, int y, char *p) {
   int n;

   buf->x = x;
   buf->y = y;
   while ((n = ssfn_render(ctx, buf, p)) > 0)
      p += n;
}

void ssfnFree(ssfn_t *ctx) {
   ssfn_free(ctx);
}
