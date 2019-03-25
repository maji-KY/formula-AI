package com.sun.jna.platform.win32;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.*;

import java.awt.*;
import java.awt.image.*;

public class MyGDI32Util extends GDI32Util {

    private static final DirectColorModel SCREENSHOT_COLOR_MODEL = new DirectColorModel(24, 0x00FF0000, 0xFF00, 0xFF);
    private static final int[] SCREENSHOT_BAND_MASKS = {
            SCREENSHOT_COLOR_MODEL.getRedMask(),
            SCREENSHOT_COLOR_MODEL.getGreenMask(),
            SCREENSHOT_COLOR_MODEL.getBlueMask()
    };

    public static BufferedImage getScreenshot(WinDef.HWND target, Rectangle jRectangle) {
        int windowWidth = jRectangle.width;
        int windowHeight = jRectangle.height;

        WinDef.HDC hdcTarget = User32.INSTANCE.GetDC(target);
        if (hdcTarget == null) {
            throw new Win32Exception(Native.getLastError());
        }

        Win32Exception we = null;

        // device context used for drawing
        WinDef.HDC hdcTargetMem = null;

        // handle to the bitmap to be drawn to
        WinDef.HBITMAP hBitmap = null;

        // original display surface associated with the device context
        WinNT.HANDLE hOriginal = null;

        // final java image structure we're returning.
        BufferedImage image = null;

        try {
            hdcTargetMem = GDI32.INSTANCE.CreateCompatibleDC(hdcTarget);
            if (hdcTargetMem == null) {
                throw new Win32Exception(Native.getLastError());
            }

            hBitmap = GDI32.INSTANCE.CreateCompatibleBitmap(hdcTarget, windowWidth, windowHeight);
            if (hBitmap == null) {
                throw new Win32Exception(Native.getLastError());
            }

            hOriginal = GDI32.INSTANCE.SelectObject(hdcTargetMem, hBitmap);
            if (hOriginal == null) {
                throw new Win32Exception(Native.getLastError());
            }

            // draw to the bitmap
            if (!GDI32.INSTANCE.BitBlt(hdcTargetMem, 0, 0, windowWidth, windowHeight, hdcTarget, 0, 0, GDI32.SRCCOPY)) {
                throw new Win32Exception(Native.getLastError());
            }

            WinGDI.BITMAPINFO bmi = new WinGDI.BITMAPINFO();
            bmi.bmiHeader.biWidth = windowWidth;
            bmi.bmiHeader.biHeight = -windowHeight;
            bmi.bmiHeader.biPlanes = 1;
            bmi.bmiHeader.biBitCount = 32;
            bmi.bmiHeader.biCompression = WinGDI.BI_RGB;

            Memory buffer = new Memory(windowWidth * windowHeight * 4);
            int resultOfDrawing = GDI32.INSTANCE.GetDIBits(hdcTarget, hBitmap, 0, windowHeight, buffer, bmi,
                    WinGDI.DIB_RGB_COLORS);
            if (resultOfDrawing == 0 || resultOfDrawing == WinError.ERROR_INVALID_PARAMETER) {
                throw new Win32Exception(Native.getLastError());
            }

            int bufferSize = windowWidth * windowHeight;
            DataBuffer dataBuffer = new DataBufferInt(buffer.getIntArray(0, bufferSize), bufferSize);
            WritableRaster raster = Raster.createPackedRaster(dataBuffer, windowWidth, windowHeight, windowWidth,
                    SCREENSHOT_BAND_MASKS, null);
            image = new BufferedImage(SCREENSHOT_COLOR_MODEL, raster, false, null);

        } catch (Win32Exception e) {
            we = e;
        } finally {
            if (hOriginal != null) {
                // per MSDN, set the display surface back when done drawing
                WinNT.HANDLE result = GDI32.INSTANCE.SelectObject(hdcTargetMem, hOriginal);
                // failure modes are null or equal to HGDI_ERROR
                if (result == null || WinGDI.HGDI_ERROR.equals(result)) {
                    Win32Exception ex = new Win32Exception(Native.getLastError());
                    if (we != null) {
                        ex.addSuppressedReflected(we);
                    }
                    we = ex;
                }
            }

            if (hBitmap != null) {
                if (!GDI32.INSTANCE.DeleteObject(hBitmap)) {
                    Win32Exception ex = new Win32Exception(Native.getLastError());
                    if (we != null) {
                        ex.addSuppressedReflected(we);
                    }
                    we = ex;
                }
            }

            if (hdcTargetMem != null) {
                // get rid of the device context when done
                if (!GDI32.INSTANCE.DeleteDC(hdcTargetMem)) {
                    Win32Exception ex = new Win32Exception(Native.getLastError());
                    if (we != null) {
                        ex.addSuppressedReflected(we);
                    }
                    we = ex;
                }
            }

            if (hdcTarget != null) {
                if (0 == User32.INSTANCE.ReleaseDC(target, hdcTarget)) {
                    throw new IllegalStateException("Device context did not release properly.");
                }
            }
        }

        if (we != null) {
            throw we;
        }
        return image;
    }
}
