package info.ruibu.util;

import org.videolan.libvlc.LibVLC;

import java.util.ArrayList;

/**
 * Created by Brooks on 2015-12-31.
 * LibVLCUtil LibVLC 单例
 */
public class LibVLCUtil {
    private static LibVLC libVLC = null;

    public synchronized static LibVLC getLibVLC(ArrayList<String> options) throws IllegalStateException {
        if (libVLC == null) {
            if (options == null) {
                libVLC = new LibVLC();
            } else {
                libVLC = new LibVLC(options);
            }
        }
        return libVLC;
    }
}