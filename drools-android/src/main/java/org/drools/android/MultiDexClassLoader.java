package org.drools.android;

import android.os.Build;
import com.android.dx.dex.DexOptions;
import com.android.dx.dex.cf.CfOptions;
import com.android.dx.dex.cf.CfTranslator;
import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import static org.drools.android.DroolsAndroidContext.*;

/**
 * Maintains separate dex files for each defined class.
 */
public class MultiDexClassLoader extends DexClassLoader {
    private static final Logger log = LoggerFactory.getLogger("DroolsAndroidContext");

    private String dexPath="";
    final DexOptions dex_options = new DexOptions();
    final CfOptions cf_options = new CfOptions();
    private static Field pathListField;
    private static Field originalPathField;
    private static Class dexPathListClazz;
    private static Constructor dexPathListConstructor;

    static {
        try {
            dexPathListClazz = Class.forName("dalvik.system.DexPathList");
            dexPathListConstructor = dexPathListClazz.getConstructor(ClassLoader.class, String.class, String.class, File.class);
            pathListField = BaseDexClassLoader.class.getDeclaredField("pathList");
            pathListField.setAccessible(true);
            originalPathField = BaseDexClassLoader.class.getDeclaredField("originalPath");
            originalPathField.setAccessible(true);
        } catch (Exception e) {
            log.error("Reflection error", e);
        }
    }

    public MultiDexClassLoader(ClassLoader parent) {
        super(new File(getDexDir(), "temp.dex").getAbsolutePath(),
                getOptimizedDir().getAbsolutePath(),
                getContext().getApplicationInfo().nativeLibraryDir,
                parent!=null ? parent : getContext().getClassLoader());
        dex_options.targetApiLevel = Build.VERSION.SDK_INT;
        cf_options.optimize = true;
    }

    /**
     * Convert class to dex
     * @param name class name
     * @param bytes   classfile bytes
     * @return  the dex file name
     */
    private String writeClass(String name, byte[] bytes) throws IOException {
        name = name.replace('.','/');
        File dexFile = new File(String.format("%s/%s.dex", getDexDir(), name));
        if(dexFile.exists() && isReuseClassFiles()) {
            if (log.isTraceEnabled())
                log.trace(String.format("Reused class [%s] from cache: %s", name, dexFile.getAbsolutePath()));
            return dexFile.getAbsolutePath();
        }
        FileOutputStream fos = null;
        try {
            //convert .class ==> .dex
            com.android.dx.dex.file.DexFile file = new com.android.dx.dex.file.DexFile(dex_options);
            file.add(CfTranslator.translate(name + ".class", bytes, cf_options, dex_options));

            //write dex file to cache dir
            dexFile.getParentFile().mkdirs();
            if (dexFile.exists()) dexFile.delete();
            fos = new FileOutputStream(dexFile);
            file.writeTo(fos, null, false);
            if (log.isTraceEnabled())
                log.trace(String.format("Wrote class [%s] to cache: %s", name, dexFile.getAbsolutePath()));
            return dexFile.getAbsolutePath();
        } finally {
            if(fos!=null) {
                try {
                    fos.close();
                } catch (IOException e) {}
            }
        }
    }

    public Class defineClass(String name, byte[] bytes) {
        try {
            String path = writeClass(name, bytes);
            dexPath += (dexPath.isEmpty() ? "" : ":") + path;
            log.trace("New Dexpath: " + dexPath);
            pathListField.set(this, dexPathListConstructor.newInstance(this,
                    path,
                    getContext().getApplicationInfo().nativeLibraryDir,
                    getOptimizedDir()));
            return findClass(name);
        } catch (Exception e) {
            log.error("Error", e);
            throw new RuntimeException(e);
        }
    }
}
