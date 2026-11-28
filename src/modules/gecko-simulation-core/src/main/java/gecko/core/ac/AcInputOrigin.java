package gecko.core.ac;

import java.util.Arrays;

/** Electrical assumptions selected for an AC input. */
public enum AcInputOrigin {
    LEGACY,
    PORTABLE_CIR,
    UNSELECTED_ASC,
    LTSPICE_26_1_1_NORMAL_27C;

    private static final String PROFILE_ID = "ltspice-26.1.1-normal-27c-v1";
    private static final String REFERENCE_CONFIGURATION =
            "d2f3ea31ea87e8473bf41c06175a9c288fe4e356c64e7485ceeac3eb6c4956bf";

    public boolean tested() { return this == LTSPICE_26_1_1_NORMAL_27C; }

    public String profileId() { return tested() ? PROFILE_ID : ""; }

    public String configurationSha256() { return tested() ? REFERENCE_CONFIGURATION : ""; }

    public double defaultSeriesResistance() {
        if (!tested()) throw new IllegalStateException("Select a tested LTspice import profile.");
        return 0.001;
    }

    public static AcInputOrigin selectProfile(String value) {
        return Arrays.stream(values()).filter(origin -> origin.tested() && origin.profileId().equals(value))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown ASC import profile " + value));
    }

    public static AcInputOrigin decode(String kind, String profile, String configuration) {
        AcInputOrigin origin = valueOf(kind);
        if (!origin.profileId().equals(profile) || !origin.configurationSha256().equals(configuration)) {
            throw new IllegalArgumentException("Invalid AC origin profile or configuration digest.");
        }
        return origin;
    }
}
