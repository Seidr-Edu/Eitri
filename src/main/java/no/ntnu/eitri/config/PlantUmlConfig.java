package no.ntnu.eitri.config;

/**
 * Full PlantUML writer configuration.
 */
public record PlantUmlConfig(
        String diagramName,
        LayoutDirection direction,
        int groupInheritance,
        int classAttributeIconSize,
        boolean hidePrivate,
        boolean hideProtected,
        boolean hidePackage,
        boolean hideFields,
        boolean hideEmptyFields,
        boolean hideMethods,
        boolean hideEmptyMethods,
        boolean hideEmptyMembers,
        boolean showCircle,
        boolean showUnlinked,
        boolean showStereotypes,
        boolean showGenerics,
        boolean showNotes,
        boolean showMultiplicities,
        boolean showLabels,
        boolean showReadOnly,
        boolean showVoidReturnTypes,
        boolean showThrows,
        boolean showInheritance,
        boolean showImplements,
        boolean showComposition,
        boolean showAggregation,
        boolean showAssociation,
        boolean showDependency,
        boolean showNested,
        boolean hideCommonPackages,
        boolean hideExternalPackages,
        boolean hideSiblingPackages) implements WriterConfig {

    public PlantUmlConfig {
        diagramName = diagramName != null ? diagramName : "diagram";
        direction = direction != null ? direction : LayoutDirection.TOP_TO_BOTTOM;
        groupInheritance = Math.max(1, groupInheritance);
        classAttributeIconSize = Math.max(0, classAttributeIconSize);
    }

    public static PlantUmlConfig defaults() {
        return new PlantUmlConfig(
                "diagram",
                LayoutDirection.TOP_TO_BOTTOM,
                2,
                0,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                true,
                true,
                true,
                true,
                true,
                false,
                true,
                true,
                true,
                true,
                false,
                true,
                true,
                true,
                true,
                true,
                true,
                false,
                true,
                false,
                false);
    }
}
