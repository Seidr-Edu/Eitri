package no.ntnu.eitri.degradation;

import no.ntnu.eitri.model.UmlField;
import no.ntnu.eitri.model.UmlGeneric;
import no.ntnu.eitri.model.UmlMethod;
import no.ntnu.eitri.model.UmlModel;
import no.ntnu.eitri.model.UmlNote;
import no.ntnu.eitri.model.UmlParameter;
import no.ntnu.eitri.model.UmlRelation;
import no.ntnu.eitri.model.UmlStereotype;
import no.ntnu.eitri.model.UmlType;

import java.util.ArrayList;

final class UmlModelCloner {

    private UmlModelCloner() {
    }

    static UmlModel cloneModel(UmlModel model) {
        UmlModel.Builder builder = UmlModel.builder()
                .name(model.getName())
                .sourcePackages(model.getSourcePackages())
                .notes(cloneNotes(model.getNotes()));

        for (UmlType type : model.getTypesSorted()) {
            builder.addType(cloneType(type));
        }
        for (UmlRelation relation : model.getRelations()) {
            builder.addRelation(cloneRelation(relation));
        }
        return builder.build();
    }

    private static ArrayList<UmlNote> cloneNotes(Iterable<UmlNote> notes) {
        ArrayList<UmlNote> clones = new ArrayList<>();
        for (UmlNote note : notes) {
            clones.add(UmlNote.builder()
                    .text(note.getText())
                    .targetTypeFqn(note.getTargetTypeFqn())
                    .targetMember(note.getTargetMember())
                    .position(note.getPosition())
                    .build());
        }
        return clones;
    }

    private static UmlType cloneType(UmlType type) {
        UmlType.Builder builder = UmlType.builder()
                .fqn(type.getFqn())
                .simpleName(type.getSimpleName())
                .alias(type.getAlias())
                .kind(type.getKind())
                .visibility(type.getVisibility())
                .style(type.getStyle())
                .outerTypeFqn(type.getOuterTypeFqn());

        for (UmlStereotype stereotype : type.getStereotypes()) {
            builder.addStereotype(new UmlStereotype(
                    stereotype.name(),
                    stereotype.spotChar(),
                    stereotype.spotColor(),
                    stereotype.values()));
        }
        for (String tag : type.getTags()) {
            builder.addTag(tag);
        }
        for (UmlGeneric generic : type.getGenerics()) {
            builder.addGeneric(new UmlGeneric(generic.identifier(), generic.bounds()));
        }
        for (UmlField field : type.getFields()) {
            builder.addField(cloneField(field));
        }
        for (UmlMethod method : type.getMethods()) {
            builder.addMethod(cloneMethod(method));
        }
        return builder.build();
    }

    private static UmlField cloneField(UmlField field) {
        return UmlField.builder()
                .name(field.getName())
                .type(field.getType())
                .typeSimpleName(field.getTypeSimpleName())
                .visibility(field.getVisibility())
                .modifiers(field.getModifiers())
                .readOnly(field.isReadOnly())
                .annotations(field.getAnnotations())
                .build();
    }

    private static UmlMethod cloneMethod(UmlMethod method) {
        UmlMethod.Builder builder = UmlMethod.builder()
                .name(method.getName())
                .returnType(method.getReturnType())
                .returnTypeSimpleName(method.getReturnTypeSimpleName())
                .visibility(method.getVisibility())
                .modifiers(method.getModifiers())
                .constructor(method.isConstructor())
                .annotations(method.getAnnotations())
                .thrownExceptions(method.getThrownExceptions())
                .generics(method.getGenerics().stream()
                        .map(generic -> new UmlGeneric(generic.identifier(), generic.bounds()))
                        .toList());

        for (UmlParameter parameter : method.getParameters()) {
            builder.addParameter(new UmlParameter(parameter.name(), parameter.type(), parameter.typeSimpleName()));
        }
        return builder.build();
    }

    private static UmlRelation cloneRelation(UmlRelation relation) {
        return UmlRelation.builder()
                .fromTypeFqn(relation.getFromTypeFqn())
                .toTypeFqn(relation.getToTypeFqn())
                .kind(relation.getKind())
                .label(relation.getLabel())
                .fromMultiplicity(relation.getFromMultiplicity())
                .toMultiplicity(relation.getToMultiplicity())
                .fromMember(relation.getFromMember())
                .toMember(relation.getToMember())
                .build();
    }
}
