package hellfrog.common;

class LongEmbedField {

    private final String name;
    private final String value;
    private final boolean inline;

    LongEmbedField(String name, String value, boolean inline) {
        this.name = name;
        this.value = value;
        this.inline = inline;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public boolean isInline() {
        return inline;
    }

    @Override
    public String toString() {
        return "LongEmbedField{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", inline=" + inline +
                '}';
    }
}
