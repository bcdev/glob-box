/**
 * This script is also included in globtoolbox.install4j. It is here only
 * for review and reformatting. (Installer -> Installation -> Screen activation -> Pre-activation script)
 */
File modulesDir = new File(context.getInstallationDirectory(), "modules");
File[] gtModuleFiles = modulesDir.listFiles(new FileFilter() {
    public boolean accept(File file) {
        String name = file.getName();
        return name.startsWith("beam-arcbingrid-reader") ||
				name.startsWith("beam-envi-reader") ||
				name.startsWith("beam-globaerosol-reader") ||
				name.startsWith("beam-globcarbon-reader") ||
				name.startsWith("beam-globcolour-reader") ||
				name.startsWith("beam-globcover-reader") ||
				name.startsWith("beam-igbp-glcc-reader") ||
				name.startsWith("beam-medspiration-reader") ||
				name.startsWith("beam-worldfire-reader") ||
				name.startsWith("glob-core") ||
				name.startsWith("glob-export") ||
				name.startsWith("glob-help") ||
				name.startsWith("glob-ui");
    }
});

if (gtModuleFiles != null && gtModuleFiles.length > 0) {
    StringBuilder sb = new StringBuilder("Following existing BEAM GlobToolbox modules will be removed:\n");
    for (int i = 0; i < gtModuleFiles.length; i++) {
        File file = gtModuleFiles[i];
        sb.append(" " + file.getName() + "\n");
    }
    Util.showMessage(sb.toString());

    sb = new StringBuilder();

    Deque<File> toDelete = new ArrayDeque<File>();
    for (int i = 0; i < gtModuleFiles.length; i++) {
        File file = gtModuleFiles[i];
        toDelete.add(file);
    }
    while (!toDelete.isEmpty()) {
        File file = toDelete.peek();
        if (file.isDirectory()) {
            File[] directoryContent = file.listFiles();
            if (directoryContent == null || directoryContent.length == 0) {
                toDelete.removeFirst();
                if (!file.delete()) {
                    sb.append(" " + file.getName() + "\n");
                }
            } else {
                for (File f : directoryContent) {
                    toDelete.addFirst(f);
                }
            }
        } else {
            toDelete.removeFirst();
            if (!file.delete()) {
                sb.append(" " + file.getName() + "\n");
            }
        }
    }

    if (sb.length() > 0) {
        Util.showMessage("Following existing BEAM GlobToolbox modules could not be removed:\n"
                + sb + "Please remove them manually from the BEAM modules directory at\n"
                + modulesDir);
    }
}
return;