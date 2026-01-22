package no.ntnu.eitri.model;

import java.util.Objects;

/**
 * A note attached to a UML element or floating.
 */
public final class UmlNote {
    private final String text;
    private final String targetTypeId;      // Attached to a type (optional)
    private final String targetMember;      // Attached to a member (optional, e.g., "fieldName" or "methodName()")
    private final NotePosition position;    // Position relative to target

    public enum NotePosition {
        LEFT("left"),
        RIGHT("right"),
        TOP("top"),
        BOTTOM("bottom");

        private final String plantUml;

        NotePosition(String plantUml) {
            this.plantUml = plantUml;
        }

        public String toPlantUml() {
            return plantUml;
        }
    }

    private UmlNote(Builder builder) {
        this.text = Objects.requireNonNull(builder.text, "Note text cannot be null");
        this.targetTypeId = builder.targetTypeId;
        this.targetMember = builder.targetMember;
        this.position = builder.position != null ? builder.position : NotePosition.RIGHT;
    }

    public String getText() {
        return text;
    }

    public String getTargetTypeId() {
        return targetTypeId;
    }

    public String getTargetMember() {
        return targetMember;
    }

    public NotePosition getPosition() {
        return position;
    }

    public boolean isFloating() {
        return targetTypeId == null;
    }

    public boolean isMemberNote() {
        return targetMember != null;
    }

    /**
     * Renders this note for PlantUML.
     * @param targetTypeName the simple name of the target type (or null for floating)
     * @return PlantUML note lines
     */
    public String toPlantUml(String targetTypeName) {
        StringBuilder sb = new StringBuilder();

        if (isFloating()) {
            // Floating note
            sb.append("note \"").append(escapeText(text)).append("\" as N");
        } else if (isMemberNote()) {
            // Note on member
            sb.append("note ").append(position.toPlantUml()).append(" of ");
            sb.append(targetTypeName).append("::").append(targetMember);
            sb.append("\n").append(text).append("\nend note");
        } else {
            // Note on type - simple single-line form if text is short
            if (!text.contains("\n") && text.length() < 40) {
                sb.append("note ").append(position.toPlantUml()).append(" of ");
                sb.append(targetTypeName).append(" : ").append(text);
            } else {
                sb.append("note ").append(position.toPlantUml()).append(" of ");
                sb.append(targetTypeName);
                sb.append("\n").append(text).append("\nend note");
            }
        }

        return sb.toString();
    }

    private String escapeText(String text) {
        return text.replace("\"", "\\\"").replace("\n", "\\n");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String text;
        private String targetTypeId;
        private String targetMember;
        private NotePosition position;

        private Builder() {}

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder targetTypeId(String targetTypeId) {
            this.targetTypeId = targetTypeId;
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
        return "UmlNote{" + (targetTypeId != null ? targetTypeId : "floating") + ": " + text + "}";
    }
}
