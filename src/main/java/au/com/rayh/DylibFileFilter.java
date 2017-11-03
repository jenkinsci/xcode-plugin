package au.com.rayh;

import java.io.File;
import java.io.FileFilter;
import java.io.Serializable;

public class DylibFileFilter implements FileFilter, Serializable {
    public static final String EXTESION = "dylib";

    public boolean accept(File pathname)  {
        return pathname.isFile() && pathname.getName().endsWith("." + EXTESION);
    }
}
