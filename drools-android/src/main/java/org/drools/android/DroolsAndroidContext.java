/*
 *
 *  * Copyright (C) 2014 CNH Industrial NV. All rights reserved.
 *  *
 *  * This software contains proprietary information of CNH Industrial NV. Neither
 *  * receipt nor possession thereof confers any right to reproduce, use, or
 *  * disclose in whole or in part any such information without written
 *  * authorization from CNH Industrial NV.
 *  *
 *
 */

package org.drools.android;

import android.content.Context;
import android.os.Build;
import com.android.dx.dex.DexOptions;
import com.android.dx.dex.cf.CfOptions;
import com.android.dx.dex.cf.CfTranslator;
import com.android.dx.dex.file.ClassDefItem;
import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexClassLoader;
import org.drools.core.util.ClassUtils;
import org.mvel2.optimizers.OptimizerFactory;
import org.mvel2.optimizers.impl.asm.ASMAccessorOptimizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

import static java.lang.Thread.currentThread;

/**
 * Created by kedzie on 6/7/14.
 */
@SuppressWarnings("PackageAccessibility")
public class DroolsAndroidContext {
    private static final Logger log = LoggerFactory.getLogger("DroolsAndroidContext");

    private static Context context;
    private static boolean reuseClassFiles = true;

    public static Context getContext() {
        return context;
    }

    public static void setContext(Context ctx) {
        context = ctx;
        cacheDir = new File(context.getCacheDir(), "drools");
//        if (cacheDir != null && cacheDir.isDirectory()) {
//            deleteDir(cacheDir);
//        }
        dexDir = new File(cacheDir, "dex");
        dexDir.mkdirs();
        optimizedDir = new File(cacheDir, "optimized");
        optimizedDir.mkdirs();

        System.setProperty("java.version", "1.6");

        OptimizerFactory.setDefaultOptimizer("ASM");
        ASMAccessorOptimizer.setMVELClassLoader(
                new DexByteArrayClassLoader((ClassLoader) ASMAccessorOptimizer.getMVELClassLoader()));
    }

    private static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    private static File cacheDir;
    private static File dexDir;
    private static File optimizedDir;

    public static boolean isReuseClassFiles() {
        return reuseClassFiles;
    }

    public static void setReuseClassFiles(boolean reuse) {
        reuseClassFiles = reuse;
    }

    public static File getCacheDir() {
        return cacheDir;
    }

    public static File getDexDir() {
        return dexDir;
    }

    public static File getOptimizedDir() {
        return optimizedDir;
    }

    /**
     * Classloader which has a single dex file with all classes inside it.
     * It is overwritten each time a new class is defined.
     */
    public static class DroolsOverwriteDexClassLoader extends DexClassLoader {

        final DexOptions dex_options = new DexOptions();
        final CfOptions cf_options = new CfOptions();
        private com.android.dx.dex.file.DexFile dexFile;
        private List<ClassDefItem> items = new LinkedList<ClassDefItem>();
        private File file;
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

        public DroolsOverwriteDexClassLoader(String name, ClassLoader parent) {
            super(new File(dexDir, name+".dex").getAbsolutePath(),
                    optimizedDir.getAbsolutePath(),
                    context.getApplicationInfo().nativeLibraryDir,
                    parent!=null ? parent : context.getClassLoader());
            file = new File(dexDir, name+".dex");
            dex_options.targetApiLevel = Build.VERSION.SDK_INT;
            cf_options.optimize = true;
        }

        protected void setName(String name) {
            file = new File(dexDir, name+".dex");
            try {
                originalPathField.set(this, file.getAbsolutePath());
            } catch (IllegalAccessException e) {}
        }

        public Class defineClass(String name, byte[] bytes) {
            log.trace(file.getName() + " classloader - Defining class " + name);
            items.add(CfTranslator.translate(name.replace('.','/') + ".class", bytes, cf_options, dex_options));
            dexFile = new com.android.dx.dex.file.DexFile(dex_options);
            for(ClassDefItem item : items)
                dexFile.add(item);

            FileOutputStream fos = null;
            try {
                if (file.exists())
                    file.delete();
                fos = new FileOutputStream(file);
                dexFile.writeTo(fos, null, false);
                pathListField.set(this, dexPathListConstructor.newInstance(this,
                        file.getAbsolutePath(),
                        context.getApplicationInfo().nativeLibraryDir,
                        optimizedDir));
                return findClass(name);
            } catch(Exception e) {
                log.error("error", e);
                throw new RuntimeException(e);
            } finally {
                if(fos!=null) {
                    try {
                        fos.close();
                    } catch (IOException e) {}
                }
            }
        }
    }

    /**
     * Maintains separate dex files for each defined class.
     */
//    public static class MultiDexClassLoader extends DexClassLoader {
//
//        private String dexPath="";
//        final DexOptions dex_options = new DexOptions();
//        final CfOptions cf_options = new CfOptions();
//        private static Field pathListField;
//        private static Field originalPathField;
//        private static Class dexPathListClazz;
//        private static Constructor dexPathListConstructor;
//
//        static {
//            try {
//                dexPathListClazz = Class.forName("dalvik.system.DexPathList");
//                dexPathListConstructor = dexPathListClazz.getConstructor(ClassLoader.class, String.class, String.class, File.class);
//                pathListField = BaseDexClassLoader.class.getDeclaredField("pathList");
//                pathListField.setAccessible(true);
//                originalPathField = BaseDexClassLoader.class.getDeclaredField("originalPath");
//                originalPathField.setAccessible(true);
//            } catch (Exception e) {
//                log.error("Reflection error", e);
//            }
//        }
//
//        public MultiDexClassLoader(ClassLoader parent) {
//            super(new File(dexDir, "temp.dex").getAbsolutePath(),
//                    optimizedDir.getAbsolutePath(),
//                    context.getApplicationInfo().nativeLibraryDir,
//                    parent!=null ? parent : context.getClassLoader());
//            dex_options.targetApiLevel = Build.VERSION.SDK_INT;
//            cf_options.optimize = true;
//        }
//
//        /**
//         * Convert class to dex
//         * @param name class name
//         * @param bytes   classfile bytes
//         * @return  the dex file name
//         */
//        private String writeClass(String name, byte[] bytes) throws IOException {
//            name = name.replace('.','/');
//            File dexFile = new File(String.format("%s/%s.dex", dexDir, name));
//            if(dexFile.exists() && isReuseClassFiles()) {
//                if (log.isTraceEnabled())
//                    log.trace(String.format("Reused class [%s] from cache: %s", name, dexFile.getAbsolutePath()));
//                return dexFile.getAbsolutePath();
//            }
//            FileOutputStream fos = null;
//            try {
//                //convert .class ==> .dex
//                com.android.dx.dex.file.DexFile file = new com.android.dx.dex.file.DexFile(dex_options);
//                file.add(CfTranslator.translate(name + ".class", bytes, cf_options, dex_options));
//
//                //write dex file to cache dir
//                dexFile.getParentFile().mkdirs();
//                if (dexFile.exists()) dexFile.delete();
//                fos = new FileOutputStream(dexFile);
//                file.writeTo(fos, null, false);
//                if (log.isTraceEnabled())
//                    log.trace(String.format("Wrote class [%s] to cache: %s", name, dexFile.getAbsolutePath()));
//                return dexFile.getAbsolutePath();
//            } finally {
//                if(fos!=null) {
//                    try {
//                        fos.close();
//                    } catch (IOException e) {}
//                }
//            }
//        }
//
//        public Class defineClass(String name, byte[] bytes) {
//            try {
//                String path = writeClass(name, bytes);
//                dexPath += (dexPath.isEmpty() ? "" : ":") + path;
//                log.trace("New Dexpath: " + dexPath);
//                pathListField.set(this, dexPathListConstructor.newInstance(this,
//                        path,
//                        context.getApplicationInfo().nativeLibraryDir,
//                        optimizedDir));
//                return findClass(name);
//            } catch (Exception e) {
//                log.error("Error", e);
//                throw new RuntimeException(e);
//            }
//        }
//    }
}
