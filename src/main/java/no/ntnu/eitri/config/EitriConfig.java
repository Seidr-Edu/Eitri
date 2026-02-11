package no.ntnu.eitri.config;

import no.ntnu.eitri.util.ExtensionNormalizer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Central configuration for Eitri diagram generation.
 * Immutable and constructed via builder.
 */
public record EitriConfig(
        List<Path> sourcePaths,
        Path outputPath,
        String parserExtension,
        String writerExtension,
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
        boolean hideCircle,
        boolean hideUnlinked,
        boolean showStereotypes,
        boolean showGenerics,
        boolean showNotes,
        boolean showMultiplicities,
        boolean showLabels,
        boolean showReadOnly,
        boolean showVoidReturnTypes,
        boolean showInheritance,
        boolean showImplements,
        boolean showComposition,
        boolean showAggregation,
        boolean showAssociation,
        boolean showDependency,
        boolean showNested,
        boolean verbose,
        boolean dryRun
) {

    public EitriConfig {
        sourcePaths = sourcePaths != null ? List.copyOf(sourcePaths) : List.of();
        diagramName = diagramName != null ? diagramName : "diagram";
        direction = direction != null ? direction : LayoutDirection.TOP_TO_BOTTOM;
        groupInheritance = Math.max(1, groupInheritance);
        classAttributeIconSize = Math.max(0, classAttributeIconSize);
        parserExtension = ExtensionNormalizer.normalizeExtension(parserExtension);
        writerExtension = ExtensionNormalizer.normalizeExtension(writerExtension);
    }

    // Compatibility getters
    public List<Path> getSourcePaths() {
        return sourcePaths;
    }

    public Path getOutputPath() {
        return outputPath;
    }

    public String getParserExtension() {
        return parserExtension;
    }

    public String getWriterExtension() {
        return writerExtension;
    }

    public String getDiagramName() {
        return diagramName;
    }

    public LayoutDirection getDirection() {
        return direction;
    }

    public int getGroupInheritance() {
        return groupInheritance;
    }

    public int getClassAttributeIconSize() {
        return classAttributeIconSize;
    }

    public boolean isHidePrivate() {
        return hidePrivate;
    }

    public boolean isHideProtected() {
        return hideProtected;
    }

    public boolean isHidePackage() {
        return hidePackage;
    }

    public boolean isHideFields() {
        return hideFields;
    }

    public boolean isHideEmptyFields() {
        return hideEmptyFields;
    }

    public boolean isHideMethods() {
        return hideMethods;
    }

    public boolean isHideEmptyMethods() {
        return hideEmptyMethods;
    }

    public boolean isHideEmptyMembers() {
        return hideEmptyMembers;
    }

    public boolean isHideCircle() {
        return hideCircle;
    }

    public boolean isHideUnlinked() {
        return hideUnlinked;
    }

    public boolean isShowStereotypes() {
        return showStereotypes;
    }

    public boolean isShowGenerics() {
        return showGenerics;
    }

    public boolean isShowNotes() {
        return showNotes;
    }

    public boolean isShowMultiplicities() {
        return showMultiplicities;
    }

    public boolean isShowLabels() {
        return showLabels;
    }

    public boolean isShowReadOnly() {
        return showReadOnly;
    }

    public boolean isShowVoidReturnTypes() {
        return showVoidReturnTypes;
    }

    public boolean isShowInheritance() {
        return showInheritance;
    }

    public boolean isShowImplements() {
        return showImplements;
    }

    public boolean isShowComposition() {
        return showComposition;
    }

    public boolean isShowAggregation() {
        return showAggregation;
    }

    public boolean isShowAssociation() {
        return showAssociation;
    }

    public boolean isShowDependency() {
        return showDependency;
    }

    public boolean isShowNested() {
        return showNested;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<Path> sourcePaths = new ArrayList<>();
        private Path outputPath;
        private String parserExtension;
        private String writerExtension;
        private String diagramName = "diagram";
        private LayoutDirection direction = LayoutDirection.TOP_TO_BOTTOM;
        private int groupInheritance = 1;
        private int classAttributeIconSize = 0;
        private boolean hidePrivate = false;
        private boolean hideProtected = false;
        private boolean hidePackage = false;
        private boolean hideFields = false;
        private boolean hideMethods = false;
        private boolean hideEmptyFields = false;
        private boolean hideEmptyMethods = false;
        private boolean hideEmptyMembers = true;
        private boolean hideCircle = false;
        private boolean hideUnlinked = false;
        private boolean showStereotypes = true;
        private boolean showGenerics = true;
        private boolean showNotes = false;
        private boolean showMultiplicities = true;
        private boolean showLabels = true;
        private boolean showReadOnly = true;
        private boolean showVoidReturnTypes = true;
        private boolean showInheritance = true;
        private boolean showImplements = true;
        private boolean showComposition = true;
        private boolean showAggregation = true;
        private boolean showAssociation = true;
        private boolean showDependency = true;
        private boolean showNested = true;
        private boolean verbose = false;
        private boolean dryRun = false;

        public Builder sourcePaths(List<Path> paths) {
            this.sourcePaths.clear();
            if (paths != null) {
                this.sourcePaths.addAll(paths);
            }
            return this;
        }

        public Builder addSourcePath(Path path) {
            if (path != null) {
                this.sourcePaths.add(path);
            }
            return this;
        }

        public Builder outputPath(Path path) {
            this.outputPath = path;
            return this;
        }

        public Builder parserExtension(String parserExtension) {
            this.parserExtension = parserExtension;
            return this;
        }

        public Builder writerExtension(String writerExtension) {
            this.writerExtension = writerExtension;
            return this;
        }

        public Builder diagramName(String name) {
            this.diagramName = name;
            return this;
        }

        public Builder direction(LayoutDirection direction) {
            this.direction = direction;
            return this;
        }

        public Builder groupInheritance(int n) {
            this.groupInheritance = n;
            return this;
        }

        public Builder classAttributeIconSize(int n) {
            this.classAttributeIconSize = n;
            return this;
        }

        public Builder hidePrivate(boolean hide) {
            this.hidePrivate = hide;
            return this;
        }

        public Builder hideProtected(boolean hide) {
            this.hideProtected = hide;
            return this;
        }

        public Builder hidePackage(boolean hide) {
            this.hidePackage = hide;
            return this;
        }

        public Builder hideFields(boolean hide) {
            this.hideFields = hide;
            return this;
        }
        
        public Builder hideEmptyFields(boolean hide) {
            this.hideEmptyFields = hide;
            return this;
        }

        public Builder hideMethods(boolean hide) {
            this.hideMethods = hide;
            return this;
        }

        public Builder hideEmptyMethods(boolean hide) {
            this.hideEmptyMethods = hide;
            return this;
        }

        public Builder hideEmptyMembers(boolean hide) {
            this.hideEmptyMembers = hide;
            return this;
        }

        public Builder hideCircle(boolean hide) {
            this.hideCircle = hide;
            return this;
        }

        public Builder hideUnlinked(boolean hide) {
            this.hideUnlinked = hide;
            return this;
        }

        public Builder showStereotypes(boolean show) {
            this.showStereotypes = show;
            return this;
        }

        public Builder showGenerics(boolean show) {
            this.showGenerics = show;
            return this;
        }

        public Builder showNotes(boolean show) {
            this.showNotes = show;
            return this;
        }

        public Builder showMultiplicities(boolean show) {
            this.showMultiplicities = show;
            return this;
        }

        public Builder showLabels(boolean show) {
            this.showLabels = show;
            return this;
        }

        public Builder showReadOnly(boolean show) {
            this.showReadOnly = show;
            return this;
        }

        public Builder showVoidReturnTypes(boolean show) {
            this.showVoidReturnTypes = show;
            return this;
        }

        public Builder showInheritance(boolean show) {
            this.showInheritance = show;
            return this;
        }

        public Builder showImplements(boolean show) {
            this.showImplements = show;
            return this;
        }

        public Builder showComposition(boolean show) {
            this.showComposition = show;
            return this;
        }

        public Builder showAggregation(boolean show) {
            this.showAggregation = show;
            return this;
        }

        public Builder showAssociation(boolean show) {
            this.showAssociation = show;
            return this;
        }

        public Builder showDependency(boolean show) {
            this.showDependency = show;
            return this;
        }

        public Builder showNested(boolean show) {
            this.showNested = show;
            return this;
        }

        public Builder verbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        public Builder dryRun(boolean dryRun) {
            this.dryRun = dryRun;
            return this;
        }

        public EitriConfig build() {
            return new EitriConfig(
                    sourcePaths,
                    outputPath,
                    parserExtension,
                    writerExtension,
                    diagramName,
                    direction,
                    groupInheritance,
                    classAttributeIconSize,
                    hidePrivate,
                    hideProtected,
                    hidePackage,
                    hideFields,
                    hideEmptyFields,
                    hideMethods,
                    hideEmptyMethods,
                    hideEmptyMembers,
                    hideCircle,
                    hideUnlinked,
                    showStereotypes,
                    showGenerics,
                    showNotes,
                    showMultiplicities,
                    showLabels,
                    showReadOnly,
                    showVoidReturnTypes,
                    showInheritance,
                    showImplements,
                    showComposition,
                    showAggregation,
                    showAssociation,
                    showDependency,
                    showNested,
                    verbose,
                    dryRun
            );
        }
    }

}
