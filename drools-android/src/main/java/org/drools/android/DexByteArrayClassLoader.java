package org.drools.android;

import org.drools.core.util.ByteArrayClassLoader;
import org.mvel2.util.MVELClassLoader;

import java.security.ProtectionDomain;

/**
 * @author kedzie
 */
public class DexByteArrayClassLoader extends MultiDexClassLoader implements ByteArrayClassLoader, MVELClassLoader {
    public DexByteArrayClassLoader(final ClassLoader parent) {
        super( parent );
    }

    public Class< ? > defineClass(final String name,
                                  final byte[] bytes,
                                  final ProtectionDomain domain) {
        return super.defineClass(name, bytes);
    }

    @Override
    public Class defineClassX(String className, byte[] b, int start, int end) {
        return super.defineClass(className, b);
    }
}
