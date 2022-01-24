package ch.bubendorf.spam;

public abstract class BuildVersion {
    public static String getBuildVersion() {
        final String version = BuildVersion.class.getPackage().getImplementationVersion();
        return version == null ? "dev" : version;
    }
}
