package SkyStudioApp;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;

public class SkyWindowFocus {
    private static final User32 user32 = User32.INSTANCE;

    public static boolean isSkyWindowFocused() {
        WinDef.HWND foregroundWindow = user32.GetForegroundWindow();
        char[] windowTitle = new char[512];
        user32.GetWindowText(foregroundWindow, windowTitle, 512);
        String activeWindowTitle = Native.toString(windowTitle);

        char[] className = new char[512];
        user32.GetClassName(foregroundWindow, className, 512);
        String windowClassName = Native.toString(className);

        return activeWindowTitle.contains("Sky") && windowClassName.equals("TgcMainWindow");
    }
}
