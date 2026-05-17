package eu.kanade.tachiyomi.shizuku;

import android.content.res.AssetFileDescriptor;

interface IShellInterface {
    void install(in AssetFileDescriptor apk) = 1;
    void destroy() = 16777114;
}
