package hellfrog.settings.entity;

public enum ACLMode {

    CLASSIC(0), OPTIONAL_ROLE(1), OPTIONAL_CHANNEL(2);

    private final long number;

    ACLMode(long number) {
        this.number = number;
    }

    public long asNumber() {
        return number;
    }

    public static ACLMode parseNumberValue(long number) {
        if (number == ACLMode.OPTIONAL_ROLE.number) {
            return ACLMode.OPTIONAL_ROLE;
        } else if (number == ACLMode.OPTIONAL_CHANNEL.number) {
            return ACLMode.OPTIONAL_CHANNEL;
        } else {
            return ACLMode.CLASSIC;
        }
    }

    public static ACLMode oldRepresentationNewMode() {
        return ACLMode.OPTIONAL_ROLE;
    }

    public static ACLMode oldRepresentationOldMode() {
        return ACLMode.CLASSIC;
    }
}
