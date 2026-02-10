package no.ntnu.eitri.model;

import java.util.Objects;

/**
 * A note attached to a UML element or floating.
 */
public final class UmlNote {
    private final String text;
    private final String targetTypeFqn;      // Attached to a type (optional)
    private final String targetMember;      // Attached to a member (optional, e.g., "fieldName" or "methodName()")
    private final NotePosition position;    // Position relative to target

    public enum NotePosition {
        LEFT,
        RIGHT,
        TOP,
        BOTTOM
    }

    private UmlNote(Builder builder) {
        this.text = Objects.requireNonNull(builder.text, "Note text cannot be null");
        this.targetTypeFqn = builder.targetTypeFqn;
        this.targetMember = builder.targetMember;
        this.position = builder.position != null ? builder.position : NotePosition.RIGHT;
    }

    public String getText() {
        return text;
    }

    public String getTargetTypeFqn() {
        return targetTypeFqn;
    }

    public String getTargetMember() {
        return targetMember;
    }

    public NotePosition getPosition() {
        return position;
    }

    public boolean isFloating() {
        return targetTypeFqn == null;
    }

    public boolean isMemberNote() {
        return targetMember != null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String text;
        private String targetTypeFqn;
        private String targetMember;
        private NotePosition position;

        private Builder() {}

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder targetTypeFqn(String targetTypeFqn) {
            this.targetTypeFqn = targetTypeFqn;
            return this;
        }

        public Builder targetMember(String targetMember) {
            this.targetMember = targetMember;
            return this;
        }

        public Builder position(NotePosition position) {
            this.position = position;
            return this;
        }

        public UmlNote build() {
            return new UmlNote(this);
        }
    }

    @Override
    public String toString() {
        return "UmlNote{" + (targetTypeFqn != null ? targetTypeFqn : "floating") + ": " + text + "}";
    }
}
