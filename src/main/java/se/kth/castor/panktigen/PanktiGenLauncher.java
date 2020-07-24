package se.kth.castor.panktigen;

import spoon.MavenLauncher;
import spoon.reflect.CtModel;

public class PanktiGenLauncher {
    public MavenLauncher getMavenLauncher(final String projectPath, final String projectName) {
        MavenLauncher launcher = new MavenLauncher(projectPath, MavenLauncher.SOURCE_TYPE.ALL_SOURCE);
        launcher.getEnvironment().setAutoImports(true);
        launcher.getEnvironment().setCommentEnabled(false);
        return launcher;
    }

    public CtModel buildSpoonModel(final MavenLauncher launcher) {
        launcher.buildModel();
        return launcher.getModel();
    }
}
