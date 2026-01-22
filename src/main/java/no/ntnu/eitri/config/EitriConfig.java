package no.ntnu.eitri.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Central configuration for Eitri diagram generation.
 * Supports builder pattern for programmatic construction and
 * YAML deserialization for config files.
 */
public final class EitriConfig {

    // === Input Settings ===
    private List<Path> sourcePaths = new ArrayList<>();
    private Path outputPath;
    private String diagramName = "diagram";

    // === Layout Settings ===
    private LayoutDirection direction = LayoutDirection.TOP_TO_BOTTOM;
    private int groupInheritance = 0;  // 0 = disabled
    private int classAttributeIconSize = 0;  // 0 = default

    // === Visibility Filtering ===
    private boolean hidePrivate = false;
    private boolean hideProtected = false;
    private boolean hidePackage = false;

    // === Member Filtering ===
    private boolean hideFields = false;
    private boolean hideMethods = false;
    private boolean hideEmptyMembers = true;

    // === Display Options ===
    private boolean hideCircle = false;
    private boolean hideUnlinked = false;
    private boolean showStereotypes = true;
    private boolean showGenerics = true;
    private boolean showNotes = false;
    private boolean showMultiplicities = true;
    private boolean showLabels = true;

    // === Relation Filtering (individual toggles) ===
    private boolean showInheritance = true;
    private boolean showImplements = true;
    private boolean showComposition = true;
    private boolean showAggregation = true;
    private boolean showAssociation = true;
    private boolean showDependency = true;

    // === Skinparam (raw lines) ===
    private List<String> skinparamLines = new ArrayList<>();

    // === Runtime Options ===
    private boolean verbose = false;
    private boolean dryRun = false;

    public EitriConfig() {
        // Default constructor for YAML deserialization
    }

    // === Getters and Setters ===

    public List<Path> getSourcePaths() {
        return sourcePaths;
    }

    public void setSourcePaths(List<Path> sourcePaths) {
        this.sourcePaths = sourcePaths != null ? sourcePaths : new ArrayList<>();
    }

    public void addSourcePath(Path path) {
        if (this.sourcePaths == null) {
            this.sourcePaths = new ArrayList<>();
        }
        this.sourcePaths.add(path);
    }

    public Path getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(Path outputPath) {
        this.outputPath = outputPath;
    }

    public String getDiagramName() {
        return diagramName;
    }

    public void setDiagramName(String diagramName) {
        this.diagramName = diagramName != null ? diagramName : "diagram";
    }

    public LayoutDirection getDirection() {
        return direction;
    }

    public void setDirection(LayoutDirection direction) {
        this.direction = direction != null ? direction : LayoutDirection.TOP_TO_BOTTOM;
    }

    public int getGroupInheritance() {
        return groupInheritance;
    }

    public void setGroupInheritance(int groupInheritance) {
        this.groupInheritance = Math.max(0, groupInheritance);
    }

    public int getClassAttributeIconSize() {
        return classAttributeIconSize;
    }

    public void setClassAttributeIconSize(int classAttributeIconSize) {
        this.classAttributeIconSize = Math.max(0, classAttributeIconSize);
    }

    public boolean isHidePrivate() {
        return hidePrivate;
    }

    public void setHidePrivate(boolean hidePrivate) {
        this.hidePrivate = hidePrivate;
    }

    public boolean isHideProtected() {
        return hideProtected;
    }

    public void setHideProtected(boolean hideProtected) {
        this.hideProtected = hideProtected;
    }

    public boolean isHidePackage() {
        return hidePackage;
    }

    public void setHidePackage(boolean hidePackage) {
        this.hidePackage = hidePackage;
    }

    public boolean isHideFields() {
        return hideFields;
    }

    public void setHideFields(boolean hideFields) {
        this.hideFields = hideFields;
    }

    public boolean isHideMethods() {
        return hideMethods;
    }

    public void setHideMethods(boolean hideMethods) {
        this.hideMethods = hideMethods;
    }

    public boolean isHideEmptyMembers() {
        return hideEmptyMembers;
    }

    public void setHideEmptyMembers(boolean hideEmptyMembers) {
        this.hideEmptyMembers = hideEmptyMembers;
    }

    public boolean isHideCircle() {
        return hideCircle;
    }

    public void setHideCircle(boolean hideCircle) {
        this.hideCircle = hideCircle;
    }

    public boolean isHideUnlinked() {
        return hideUnlinked;
    }

    public void setHideUnlinked(boolean hideUnlinked) {
        this.hideUnlinked = hideUnlinked;
    }

    public boolean isShowStereotypes() {
        return showStereotypes;
    }

    public void setShowStereotypes(boolean showStereotypes) {
        this.showStereotypes = showStereotypes;
    }

    public boolean isShowGenerics() {
        return showGenerics;
    }

    public void setShowGenerics(boolean showGenerics) {
        this.showGenerics = showGenerics;
    }

    public boolean isShowNotes() {
        return showNotes;
    }

    public void setShowNotes(boolean showNotes) {
        this.showNotes = showNotes;
    }

    public boolean isShowMultiplicities() {
        return showMultiplicities;
    }

    public void setShowMultiplicities(boolean showMultiplicities) {
        this.showMultiplicities = showMultiplicities;
    }

    public boolean isShowLabels() {
        return showLabels;
    }

    public void setShowLabels(boolean showLabels) {
        this.showLabels = showLabels;
    }

    public boolean isShowInheritance() {
        return showInheritance;
    }

    public void setShowInheritance(boolean showInheritance) {
        this.showInheritance = showInheritance;
    }

    public boolean isShowImplements() {
        return showImplements;
    }

    public void setShowImplements(boolean showImplements) {
        this.showImplements = showImplements;
    }

    public boolean isShowComposition() {
        return showComposition;
    }

    public void setShowComposition(boolean showComposition) {
        this.showComposition = showComposition;
    }

    public boolean isShowAggregation() {
        return showAggregation;
    }

    public void setShowAggregation(boolean showAggregation) {
        this.showAggregation = showAggregation;
    }

    public boolean isShowAssociation() {
        return showAssociation;
    }

    public void setShowAssociation(boolean showAssociation) {
        this.showAssociation = showAssociation;
    }

    public boolean isShowDependency() {
        return showDependency;
    }

    public void setShowDependency(boolean showDependency) {
        this.showDependency = showDependency;
    }

    public List<String> getSkinparamLines() {
        return skinparamLines;
    }

    public void setSkinparamLines(List<String> skinparamLines) {
        this.skinparamLines = skinparamLines != null ? skinparamLines : new ArrayList<>();
    }

    public void addSkinparamLine(String line) {
        if (this.skinparamLines == null) {
            this.skinparamLines = new ArrayList<>();
        }
        this.skinparamLines.add(line);
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    // === Builder ===

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final EitriConfig config = new EitriConfig();

        public Builder addSourcePath(Path path) {
            config.addSourcePath(path);
            return this;
        }

        public Builder outputPath(Path path) {
            config.setOutputPath(path);
            return this;
        }

        public Builder diagramName(String name) {
            config.setDiagramName(name);
            return this;
        }

        public Builder direction(LayoutDirection direction) {
            config.setDirection(direction);
            return this;
        }

        public Builder groupInheritance(int n) {
            config.setGroupInheritance(n);
            return this;
        }

        public Builder classAttributeIconSize(int n) {
            config.setClassAttributeIconSize(n);
            return this;
        }

        public Builder hidePrivate(boolean hide) {
            config.setHidePrivate(hide);
            return this;
        }

        public Builder hideProtected(boolean hide) {
            config.setHideProtected(hide);
            return this;
        }

        public Builder hidePackage(boolean hide) {
            config.setHidePackage(hide);
            return this;
        }

        public Builder hideFields(boolean hide) {
            config.setHideFields(hide);
            return this;
        }

        public Builder hideMethods(boolean hide) {
            config.setHideMethods(hide);
            return this;
        }

        public Builder hideEmptyMembers(boolean hide) {
            config.setHideEmptyMembers(hide);
            return this;
        }

        public Builder hideCircle(boolean hide) {
            config.setHideCircle(hide);
            return this;
        }

        public Builder hideUnlinked(boolean hide) {
            config.setHideUnlinked(hide);
            return this;
        }

        public Builder showStereotypes(boolean show) {
            config.setShowStereotypes(show);
            return this;
        }

        public Builder showGenerics(boolean show) {
            config.setShowGenerics(show);
            return this;
        }

        public Builder showNotes(boolean show) {
            config.setShowNotes(show);
            return this;
        }

        public Builder showMultiplicities(boolean show) {
            config.setShowMultiplicities(show);
            return this;
        }

        public Builder showLabels(boolean show) {
            config.setShowLabels(show);
            return this;
        }

        public Builder showInheritance(boolean show) {
            config.setShowInheritance(show);
            return this;
        }

        public Builder showImplements(boolean show) {
            config.setShowImplements(show);
            return this;
        }

        public Builder showComposition(boolean show) {
            config.setShowComposition(show);
            return this;
        }

        public Builder showAggregation(boolean show) {
            config.setShowAggregation(show);
            return this;
        }

        public Builder showAssociation(boolean show) {
            config.setShowAssociation(show);
            return this;
        }

        public Builder showDependency(boolean show) {
            config.setShowDependency(show);
            return this;
        }

        public Builder addSkinparamLine(String line) {
            config.addSkinparamLine(line);
            return this;
        }

        public Builder verbose(boolean verbose) {
            config.setVerbose(verbose);
            return this;
        }

        public Builder dryRun(boolean dryRun) {
            config.setDryRun(dryRun);
            return this;
        }

        public EitriConfig build() {
            return config;
        }
    }

    @Override
    public String toString() {
        return "EitriConfig{" +
                "sourcePaths=" + sourcePaths +
                ", outputPath=" + outputPath +
                ", diagramName='" + diagramName + '\'' +
                ", direction=" + direction +
                ", hidePrivate=" + hidePrivate +
                ", hideEmptyMembers=" + hideEmptyMembers +
                ", verbose=" + verbose +
                '}';
    }
}
