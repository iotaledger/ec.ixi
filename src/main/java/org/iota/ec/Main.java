package org.iota.ec;

import org.iota.ict.Ict;
import org.iota.ict.utils.properties.Properties;

public class Main {

    public static void main(String[] args) {
        Ict ict = new Ict(new Properties().toFinal());
        try {
            ict.getModuleHolder().loadVirtualModule(ECModule.class, "ec.ixi-1.0.jar");
        } catch (Throwable t) {
            t.printStackTrace();
        }
        System.out.println(ict.getModuleHolder().getModule("ec.ixi-1.0.jar") == null);
        ict.getModuleHolder().startAllModules();
    }
}
