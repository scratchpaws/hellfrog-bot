package besus.utils;

public interface SelfTyped <SELF extends SelfTyped<SELF>> {
    default SELF getSelf() {
        return (SELF) this;
    };
}
