package no.ntnu.eitri.writer.plantuml;

import no.ntnu.eitri.model.Modifier;
import no.ntnu.eitri.model.RelationKind;
import no.ntnu.eitri.model.TypeKind;
import no.ntnu.eitri.model.UmlField;
import no.ntnu.eitri.model.UmlGeneric;
import no.ntnu.eitri.model.UmlMethod;
import no.ntnu.eitri.model.UmlParameter;
import no.ntnu.eitri.model.UmlRelation;
import no.ntnu.eitri.model.UmlStereotype;
import no.ntnu.eitri.model.UmlType;
import no.ntnu.eitri.model.Visibility;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Renders UML model elements into PlantUML syntax.
 */
public final class PlantUmlRenderer {

    /**
     * Return FQN display name for the type to be used for consistent referencing.
     */
    public String displayNameForType(UmlType type) {
        if (!type.isNested()) {
            return type.getFqn();
        }

        // For nested types, extract the nested path from FQN
        String fqn = type.getFqn();
        int lastDot = fqn.lastIndexOf('.');
        if (lastDot < 0) {
            return type.getFqn();
        }

        // Find where the type hierarchy starts (first uppercase segment)
        String[] parts = fqn.split("\\.");
        StringBuilder nestedName = new StringBuilder();
        boolean foundFirstType = false;

        for (String part : parts) {
            if (!part.isEmpty() && Character.isUpperCase(part.charAt(0))) {
                if (foundFirstType) {
                    nestedName.append("$");
                }
                nestedName.append(part);
                foundFirstType = true;
            }
        }

        if (!nestedName.isEmpty()) {
            String packageName = type.getPackageName();
            return (packageName == null || packageName.isEmpty())
                    ? nestedName.toString()
                    : packageName.concat(".").concat(nestedName.toString());
        }
        return type.getFqn();
    }

    public String renderTypeDeclaration(UmlType type) {
        StringBuilder sb = new StringBuilder();

        sb.append(visibilitySymbol(type.getVisibility()));
        sb.append(typeKeyword(type.getKind())).append(" ");

        // Use alias if explicitly set, otherwise use display name with $ for nested
        // types
        if (type.getAlias() != null) {
            sb.append("\"").append(type.getAlias()).append("\" as ").append(displayNameForType(type));
        } else {
            sb.append(displayNameForType(type));
        }

        if (!type.getGenerics().isEmpty()) {
            String genericStr = type.getGenerics().stream()
                    .map(this::renderGeneric)
                    .collect(Collectors.joining(", "));
            sb.append("<").append(genericStr).append(">");
        }

        for (UmlStereotype stereotype : type.getStereotypes()) {
            sb.append(" ").append(renderStereotype(stereotype));
        }

        for (String tag : type.getTags()) {
            sb.append(" $").append(tag);
        }

        if (type.getStyle() != null && !type.getStyle().isBlank()) {
            sb.append(" ").append(type.getStyle());
        }

        return sb.toString();
    }

    public String renderField(UmlField field, boolean showReadOnly) {
        StringBuilder sb = new StringBuilder();

        sb.append(visibilitySymbol(field.getVisibility()));

        String modifiers = renderModifiers(field.getModifiers());
        if (!modifiers.isEmpty()) {
            sb.append(modifiers).append(" ");
        }

        sb.append(field.getName()).append(" : ").append(field.getTypeSimpleName());

        if (field.isReadOnly() && showReadOnly) {
            sb.append(" {readOnly}");
        }

        return sb.toString();
    }

    public String renderMethod(UmlMethod method, boolean showVoidReturnTypes) {
        StringBuilder sb = new StringBuilder();

        sb.append(visibilitySymbol(method.getVisibility()));

        String modifiers = renderModifiers(method.getModifiers());
        if (!modifiers.isEmpty()) {
            sb.append(modifiers).append(" ");
        }

        sb.append(renderMethodSignature(method));

        boolean showReturnType = !method.isConstructor()
                && (showVoidReturnTypes || !"void".equals(method.getReturnTypeSimpleName()));
        if (showReturnType) {
            sb.append(" : ").append(method.getReturnTypeSimpleName());
        }

        return sb.toString();
    }

    public String renderRelation(UmlRelation relation,
            String fromTypeName,
            String toTypeName,
            boolean showLabels,
            boolean showMultiplicities) {
        StringBuilder sb = new StringBuilder();

        String leftSide;
        String rightSide;

        if (relation.getKind().isHierarchy()) {
            leftSide = toTypeName;
            rightSide = fromTypeName;
        } else {
            leftSide = fromTypeName;
            rightSide = toTypeName;
        }

        if (relation.isMemberRelation()) {
            sb.append(fromTypeName).append("::").append(relation.getFromMember());
            sb.append(" ").append(relationArrow(relation.getKind())).append(" ");
            sb.append(toTypeName).append("::").append(relation.getToMember());
        } else {
            sb.append(leftSide);
            if (showMultiplicities && !relation.getKind().isHierarchy() && relation.getFromMultiplicity() != null) {
                sb.append(" \"").append(relation.getFromMultiplicity()).append("\"");
            }

            sb.append(" ").append(relationArrow(relation.getKind())).append(" ");

            if (showMultiplicities && !relation.getKind().isHierarchy() && relation.getToMultiplicity() != null) {
                sb.append("\"").append(relation.getToMultiplicity()).append("\" ");
            }
            sb.append(rightSide);
        }

        String relationLabel = resolveRelationLabel(relation);
        if (showLabels && relationLabel != null && !relationLabel.isBlank()) {
            sb.append(" : ").append(relationLabel);
        }

        return sb.toString();
    }

    private String resolveRelationLabel(UmlRelation relation) {
        if (relation.getLabel() != null && !relation.getLabel().isBlank()) {
            return relation.getLabel();
        }

        // Field/member-origin relations often only set fromMember.
        // Use it as a fallback label so showLabels=true produces useful output.
        // TODO: Consider if this is the best approach. Types not modeled due to config
        // settings might leak here.
        if (!relation.isMemberRelation() && relation.getFromMember() != null && !relation.getFromMember().isBlank()) {
            return relation.getFromMember();
        }

        return null;
    }

    private String renderMethodSignature(UmlMethod method) {
        String params = method.getParameters().stream()
                .map(this::renderParameter)
                .collect(Collectors.joining(", "));
        return method.getName() + "(" + params + ")";
    }

    private String renderParameter(UmlParameter parameter) {
        if (parameter.typeSimpleName() == null
                || parameter.typeSimpleName().isBlank()
                || "void".equals(parameter.typeSimpleName())) {
            return parameter.name();
        }
        return parameter.name() + ": " + parameter.typeSimpleName();
    }

    private String renderGeneric(UmlGeneric generic) {
        if (generic.bounds() != null && !generic.bounds().isBlank()) {
            return generic.identifier() + " " + generic.bounds();
        }
        return generic.identifier();
    }

    private String renderStereotype(UmlStereotype stereotype) {
        if (stereotype.spotChar() != null && stereotype.spotColor() != null) {
            return "<< (" + stereotype.spotChar() + "," + stereotype.spotColor() + ") " + stereotype.name() + " >>";
        } else if (stereotype.spotChar() != null) {
            return "<< (" + stereotype.spotChar() + ") " + stereotype.name() + " >>";
        }
        return "<<" + stereotype.name() + ">>";
    }

    private String renderModifiers(Set<Modifier> modifiers) {
        if (modifiers == null || modifiers.isEmpty()) {
            return "";
        }

        return modifiers.stream()
                .map(this::modifierToken)
                .filter(token -> !token.isEmpty())
                .collect(Collectors.joining(" "));
    }

    private String modifierToken(Modifier modifier) {
        return switch (modifier) {
            case STATIC -> "{static}";
            case ABSTRACT -> "{abstract}";
            default -> "";
        };
    }

    private String visibilitySymbol(Visibility visibility) {
        return switch (visibility) {
            case PUBLIC -> "+";
            case PRIVATE -> "-";
            case PROTECTED -> "#";
            case PACKAGE -> "~";
        };
    }

    private String relationArrow(RelationKind kind) {
        return switch (kind) {
            case EXTENDS -> "<|--";
            case IMPLEMENTS -> "<|..";
            case COMPOSITION -> "*--";
            case AGGREGATION -> "o--";
            case ASSOCIATION -> "--";
            case DEPENDENCY -> "..>";
            case NESTED -> "+--";
        };
    }

    private String typeKeyword(TypeKind kind) {
        return switch (kind) {
            case CLASS -> "class";
            case ABSTRACT_CLASS -> "abstract class";
            case INTERFACE -> "interface";
            case ENUM -> "enum";
            case ANNOTATION -> "annotation";
            case RECORD -> "class"; // PlantUML does not have a specific 'record' keyword
        };
    }
}
